package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.CandidateProfileDTO;
import com.jobmatcher.api.dto.CandidateProfileUpsertRequest;
import com.jobmatcher.api.service.CandidateProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates/me")
public class CandidateProfileController {

    private final CandidateProfileService service;

    public CandidateProfileController(CandidateProfileService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CANDIDATE','DEV','ADMIN')")
    public CandidateProfileDTO getMe() {
        return service.getMe();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('CANDIDATE','DEV','ADMIN')")
    public CandidateProfileDTO upsertMe(@RequestBody CandidateProfileUpsertRequest req) {
        return service.upsertMe(req);
    }
}
