package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record JobDTO(
        UUID id,
        String ownerUsername,
        String title,
        String description,
        String location,
        Double lat,
        Double lon,
        String contractType,
        String seniority,
        String status,
        Instant publishedAt,
        Instant archivedAt,
        String applyUrl,
        boolean embedded,
        String embeddingModel,
        Instant embeddingUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {}
