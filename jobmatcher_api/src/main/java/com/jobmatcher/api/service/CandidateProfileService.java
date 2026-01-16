package com.jobmatcher.api.service;

import com.jobmatcher.api.domain.candidate.CandidateProfile;
import com.jobmatcher.api.dto.CandidateProfileDTO;
import com.jobmatcher.api.dto.CandidateProfileUpsertRequest;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.exception.NotFoundException;
import com.jobmatcher.api.repository.CandidateProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository repo;

    public CandidateProfileService(CandidateProfileRepository repo) {
        this.repo = repo;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public CandidateProfileDTO getMe() {
        String username = currentUsername();
        CandidateProfile p = repo.findByOwnerUsername(username)
                .orElseThrow(() -> new NotFoundException("Profilo candidato non trovato"));
        return toDto(p);
    }

    public CandidateProfileDTO upsertMe(CandidateProfileUpsertRequest req) {
        String username = currentUsername();

        CandidateProfile p = repo.findByOwnerUsername(username).orElseGet(() -> CandidateProfile.builder()
                .ownerUsername(username)
                .build());

        // campi aggiornabili (tutti opzionali)
        if (req != null) {
            p.setFirstName(req.firstName());
            p.setLastName(req.lastName());
            p.setEmail(req.email());
            p.setPhone(req.phone());
            p.setLocation(req.location());
        }

        CandidateProfile saved = repo.save(p);
        return toDto(saved);
    }

    private CandidateProfileDTO toDto(CandidateProfile p) {
        return new CandidateProfileDTO(
                p.getId(),
                p.getOwnerUsername(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getPhone(),
                p.getLocation(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
