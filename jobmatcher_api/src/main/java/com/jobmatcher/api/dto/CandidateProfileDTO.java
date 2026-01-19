package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CandidateProfileDTO(
        UUID id,
        String ownerUsername,
        String firstName,
        String lastName,
        String email,
        String phone,
        String location,
        UUID activeCvFileId,
        Instant createdAt,
        Instant updatedAt
) {}
