package com.example.studentnews.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class NewsApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("student_news_test")
            .withUsername("student")
            .withPassword("student");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLifecycleShouldWork() throws Exception {
        String createdRaw = mockMvc.perform(post("/api/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  New semester starts  ",
                                  "content": "  Lessons begin next Monday.  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createdRaw);
        long id = created.get("id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");
        assertThat(created.get("title").asText()).isEqualTo("New semester starts");

        mockMvc.perform(put("/api/news/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "content": "Updated content"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/news/{id}/publish", id))
                .andExpect(status().isOk());

        String publishedListRaw = mockMvc.perform(get("/api/news?status=PUBLISHED"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode publishedList = objectMapper.readTree(publishedListRaw);
        assertThat(publishedList.isArray()).isTrue();
        assertThat(publishedList.size()).isEqualTo(1);
        assertThat(publishedList.get(0).get("id").asLong()).isEqualTo(id);

        mockMvc.perform(patch("/api/news/{id}/archive", id))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/news/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk());
    }

    @Test
    void createShouldReturnBadRequestWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
