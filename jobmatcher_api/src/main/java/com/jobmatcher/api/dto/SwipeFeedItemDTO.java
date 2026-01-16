package com.jobmatcher.api.dto;

public record SwipeFeedItemDTO(
        JobDTO job,
        Double distanceKm,
        Double score
) {}
