package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.MatchItemDTO;
import com.jobmatcher.api.service.CandidateMatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates/me")
public class CandidateMatchController {

    private final CandidateMatchService service;

    public CandidateMatchController(CandidateMatchService service) {
        this.service = service;
    }

    @GetMapping("/matches")
    @PreAuthorize("hasAnyRole('CANDIDATE','DEV','ADMIN')")
    public List<MatchItemDTO> matches(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Integer limit
    ) {
        return service.getMatches(lat, lon, radiusKm, limit);
    }
}
