package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.company.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {
    Optional<CompanyProfile> findByOwnerUsername(String ownerUsername);
}
