package com.jobmatcher.api.dto;

import java.util.List;

public record EmbedResponse(
        List<Double> embedding,
        String model_used
) {}

