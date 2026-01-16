package com.jobmatcher.api.dto;

public record JobCreateRequest(
        String title,
        String description,
        String location,
        Double lat,
        Double lon,
        String contractType,
        String seniority,
        String status,     // "DRAFT" / "PUBLISHED" / "ARCHIVED" (opzionale)
        String applyUrl
) {}
