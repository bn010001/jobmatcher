package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvFileRepository extends JpaRepository<CvFile, UUID> {
    List<CvFile> findByOwnerUsernameOrderByUploadedAtDesc(String ownerUsername);
    Optional<CvFile> findByIdAndOwnerUsername(UUID id, String ownerUsername);
    List<CvFile> findAllByOrderByUploadedAtDesc();
    Optional<CvFile> findFirstByOwnerUsernameAndStatusOrderByUploadedAtDesc(String ownerUsername, CvProcessingStatus status);

}
