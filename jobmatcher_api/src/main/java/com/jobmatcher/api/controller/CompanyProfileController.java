package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.CompanyProfileDTO;
import com.jobmatcher.api.dto.CompanyProfileUpsertRequest;
import com.jobmatcher.api.service.CompanyProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies/me")
public class CompanyProfileController {

    private final CompanyProfileService service;

    public CompanyProfileController(CompanyProfileService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public CompanyProfileDTO getMe() {
        return service.getMe();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public CompanyProfileDTO upsertMe(@RequestBody CompanyProfileUpsertRequest req) {
        return service.upsertMe(req);
    }
}
