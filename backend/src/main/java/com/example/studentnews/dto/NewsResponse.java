package com.example.studentnews.dto;

import com.example.studentnews.entity.NewsStatus;

import java.time.Instant;

public record NewsResponse(
        Long id,
        String title,
        String content,
        NewsStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
