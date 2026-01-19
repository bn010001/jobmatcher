package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.CompanyMatchItemDTO;
import com.jobmatcher.api.service.CompanyMatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/me")
public class CompanyMatchController {

    private final CompanyMatchService service;

    public CompanyMatchController(CompanyMatchService service) {
        this.service = service;
    }

    @GetMapping("/matches")
    @PreAuthorize("hasAnyRole('COMPANY','DEV','ADMIN')")
    public List<CompanyMatchItemDTO> matches(@RequestParam(required = false) Integer limit) {
        return service.getCompanyMatches(limit);
    }
}
