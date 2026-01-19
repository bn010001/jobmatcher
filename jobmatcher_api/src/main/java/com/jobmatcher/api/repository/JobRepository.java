package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.job.Job;
import com.jobmatcher.api.domain.job.JobStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    Optional<Job> findByIdAndOwnerUsername(UUID id, String ownerUsername);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findByOwnerUsernameAndStatusOrderByCreatedAtDesc(String ownerUsername, JobStatus status);


}
