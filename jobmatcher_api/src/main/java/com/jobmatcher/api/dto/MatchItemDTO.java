package com.jobmatcher.api.dto;

import java.util.List;

public record MatchItemDTO(
        JobDTO job,
        Double distanceKm,
        Double score,
        List<String> reasons // keywords in comune (demo UX)
) {}
