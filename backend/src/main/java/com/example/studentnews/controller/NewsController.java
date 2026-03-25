package com.example.studentnews.controller;

import com.example.studentnews.dto.NewsRequest;
import com.example.studentnews.dto.NewsResponse;
import com.example.studentnews.entity.NewsStatus;
import com.example.studentnews.exception.NotFoundException;
import com.example.studentnews.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "Endpoints for managing student news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    @Operation(summary = "List news", description = "Returns all news or filters by status when provided")
    @ApiResponse(
            responseCode = "200",
            description = "News list retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NewsResponse.class)))
    )
    public List<NewsResponse> list(@RequestParam(required = false) NewsStatus status) {
        return newsService.list(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create news draft", description = "Creates a new news item with DRAFT status")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "News draft created",
                    content = @Content(schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public NewsResponse create(@Valid @RequestBody NewsRequest request) {
        return newsService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit news", description = "Updates title and content for an existing news item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News updated",
                    content = @Content(schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "News not found")
    })
    public NewsResponse update(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return newsService.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish news", description = "Changes news status to PUBLISHED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News published",
                    content = @Content(schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "News not found")
    })
    public NewsResponse publish(@PathVariable Long id) {
        return newsService.publish(id);
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive news", description = "Changes news status to ARCHIVED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News archived",
                    content = @Content(schema = @Schema(implementation = NewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "News not found")
    })
    public NewsResponse archive(@PathVariable Long id) {
        return newsService.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete news", description = "Deletes a news item by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "News deleted"),
            @ApiResponse(responseCode = "404", description = "News not found")
    })
    public void delete(@PathVariable Long id) {
        newsService.delete(id);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Operation(hidden = true)
    public Map<String, String> handleNotFound(NotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }
}
