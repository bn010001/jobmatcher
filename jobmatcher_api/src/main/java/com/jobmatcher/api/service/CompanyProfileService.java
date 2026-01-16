package com.jobmatcher.api.service;

import com.jobmatcher.api.domain.company.CompanyProfile;
import com.jobmatcher.api.dto.CompanyProfileDTO;
import com.jobmatcher.api.dto.CompanyProfileUpsertRequest;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.exception.NotFoundException;
import com.jobmatcher.api.repository.CompanyProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CompanyProfileService {

    private final CompanyProfileRepository repo;

    public CompanyProfileService(CompanyProfileRepository repo) {
        this.repo = repo;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public CompanyProfileDTO getMe() {
        String username = currentUsername();
        CompanyProfile p = repo.findByOwnerUsername(username)
                .orElseThrow(() -> new NotFoundException("Profilo azienda non trovato"));
        return toDto(p);
    }

    public CompanyProfileDTO upsertMe(CompanyProfileUpsertRequest req) {
        if (req == null || req.companyName() == null || req.companyName().trim().isEmpty()) {
            throw new BadRequestException("companyName è obbligatorio");
        }

        String username = currentUsername();

        CompanyProfile p = repo.findByOwnerUsername(username).orElseGet(() -> CompanyProfile.builder()
                .ownerUsername(username)
                .build());

            p.setCompanyName(req.companyName().trim());
            p.setWebsite(trimToNull(req.website()));
            p.setIndustry(trimToNull(req.industry()));
            p.setLocation(trimToNull(req.location()));

            p.setContactName(trimToNull(req.contactName()));
            p.setContactEmail(trimToNull(req.contactEmail()));
            p.setContactPhone(trimToNull(req.contactPhone()));

            if (p.getContactEmail() != null && !isValidEmail(p.getContactEmail())) {
                throw new BadRequestException("contactEmail non valida");
            }

        CompanyProfile saved = repo.save(p);
        return toDto(saved);
    }

    private CompanyProfileDTO toDto(CompanyProfile p) {
        return new CompanyProfileDTO(
                p.getId(),
                p.getOwnerUsername(),
                p.getCompanyName(),
                p.getWebsite(),
                p.getIndustry(),
                p.getLocation(),
                p.getContactName(),
                p.getContactEmail(),
                p.getContactPhone(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isValidEmail(String email) {
        // “good enough” per demo: evita cose palesemente sbagliate
        // (non è RFC-perfect, ma va benissimo qui)
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
