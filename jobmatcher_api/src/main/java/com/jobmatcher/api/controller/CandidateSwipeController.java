package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.SwipeFeedItemDTO;
import com.jobmatcher.api.dto.SwipeRequest;
import com.jobmatcher.api.dto.SwipeResponse;
import com.jobmatcher.api.service.CandidateSwipeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates/me")
public class CandidateSwipeController {

    private final CandidateSwipeService service;

    public CandidateSwipeController(CandidateSwipeService service) {
        this.service = service;
    }

    @GetMapping("/swipe-feed")
    @PreAuthorize("hasAnyRole('CANDIDATE','DEV','ADMIN')")
    public List<SwipeFeedItemDTO> swipeFeed(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Integer limit
    ) {
        return service.getSwipeFeed(lat, lon, radiusKm, limit);
    }

    @PostMapping("/swipes")
    @PreAuthorize("hasAnyRole('CANDIDATE','DEV','ADMIN')")
    public SwipeResponse swipe(@RequestBody SwipeRequest req) {
        return service.swipe(req);
    }
}

