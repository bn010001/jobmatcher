package com.jobmatcher.api.dto;

import java.util.UUID;

public record SwipeRequest(
        UUID jobId,
        String action // LIKE / DISLIKE
) {}
