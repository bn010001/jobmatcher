package com.jobmatcher.api.dto;

public record CompanyProfileUpsertRequest(
        String companyName,
        String website,
        String industry,
        String location,
        String contactName,
        String contactEmail,
        String contactPhone
) {}
