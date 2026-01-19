package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvFileRepository extends JpaRepository<CvFile, UUID> {
    List<CvFile> findByOwnerUsernameOrderByUploadedAtDesc(String ownerUsername);
    Optional<CvFile> findByIdAndOwnerUsername(UUID id, String ownerUsername);
    List<CvFile> findAllByOrderByUploadedAtDesc();
    Optional<CvFile> findByIdAndOwnerUsernameAndStatus(UUID id, String ownerUsername, CvProcessingStatus status);

    Optional<CvFile> findFirstByOwnerUsernameAndStatusOrderByUploadedAtDesc(String ownerUsername, CvProcessingStatus status);
}
