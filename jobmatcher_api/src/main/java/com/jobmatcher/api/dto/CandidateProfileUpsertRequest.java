package com.jobmatcher.api.dto;

public record CandidateProfileUpsertRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String location
) {}

