package com.jobmatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyProfileDTO(
        UUID id,
        String ownerUsername,
        String companyName,
        String website,
        String industry,
        String location,
        String contactName,
        String contactEmail,
        String contactPhone,
        Instant createdAt,
        Instant updatedAt
) {}
