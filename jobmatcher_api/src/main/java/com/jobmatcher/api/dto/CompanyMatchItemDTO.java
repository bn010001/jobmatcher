package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyMatchItemDTO(
        UUID jobId,
        String jobTitle,
        String candidateUsername,
        Double score,
        List<String> reasons,
        Instant likedAt
) {}
