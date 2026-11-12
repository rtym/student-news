package com.example.avatars;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class AvatarServiceApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(AvatarServiceApplication.class, args);
    }

    /**
     * Tomcat refuses a file over MAX_BYTES before the controller runs. The whole
     * request may be a little larger than the file: the multipart wrapping
     * around it needs some room.
     */
    @Bean
    MultipartConfigElement multipartConfig(@Value("${avatars.max-bytes}") long maxBytes) {
        return new MultipartConfigElement("", maxBytes, maxBytes + 64 * 1024, 0);
    }

    /** The news UI calls this service straight from the browser, like the backend. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/avatars/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location");
    }
}
