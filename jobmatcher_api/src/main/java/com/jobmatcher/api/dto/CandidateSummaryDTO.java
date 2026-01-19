package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.List;

public record CandidateSummaryDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        List<String> topSkills,
        Instant cvUpdatedAt
) {}
