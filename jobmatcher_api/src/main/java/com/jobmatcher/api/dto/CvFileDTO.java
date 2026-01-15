package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CvFileDTO(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt,
        Instant analyzedAt,
        String status,
        String errorMessage
) {}

