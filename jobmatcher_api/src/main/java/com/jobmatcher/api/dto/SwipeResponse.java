package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SwipeResponse(
        UUID jobId,
        String action,
        Instant createdAt
) {}

