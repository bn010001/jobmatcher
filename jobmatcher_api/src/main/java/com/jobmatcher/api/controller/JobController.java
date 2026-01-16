package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.JobCreateRequest;
import com.jobmatcher.api.dto.JobDTO;
import com.jobmatcher.api.service.JobService;
import com.jobmatcher.api.dto.JobStatusUpdateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    // Company creates jobs (DEV/ADMIN allowed)
    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public JobDTO create(@RequestBody JobCreateRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public JobDTO updateStatus(@PathVariable UUID id, @RequestBody JobStatusUpdateRequest req) {
        return service.updateStatus(id, req != null ? req.status() : null);
    }

    // Public listing for candidates (published only)
    @GetMapping
    @PreAuthorize("hasAnyRole('CANDIDATE','COMPANY','DEV','ADMIN')")
    public List<JobDTO> listPublished() {
        return service.listPublished();
    }

    // Company “mine” listing
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public List<JobDTO> listMine() {
        return service.listMine();
    }

    // Public get (published/non-published visibility: per demo lasciamo semplice)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE','COMPANY','DEV','ADMIN')")
    public JobDTO getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    // If you want strict ownership checks for non-dev:
    @GetMapping("/mine/{id}")
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public JobDTO getMineById(@PathVariable UUID id) {
        return service.getMineById(id);
    }
}

