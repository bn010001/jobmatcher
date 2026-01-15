package com.jobmatcher.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record CvAnalysisDTO(
        Instant analyzedAt,
        JsonNode analysisJson,
        String status,
        String errorMessage
) {}

