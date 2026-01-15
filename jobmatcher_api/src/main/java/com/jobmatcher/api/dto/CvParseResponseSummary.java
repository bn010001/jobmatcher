package com.jobmatcher.api.dto;

public record CvParseResponseSummary(
        String text,
        Object sections,
        String modelUsed
) {
    
}

