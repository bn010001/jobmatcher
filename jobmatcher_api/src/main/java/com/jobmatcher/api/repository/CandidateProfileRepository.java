package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.candidate.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByOwnerUsername(String ownerUsername);
}
