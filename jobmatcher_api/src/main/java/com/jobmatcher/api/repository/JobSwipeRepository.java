package com.jobmatcher.api.repository;

import com.jobmatcher.api.domain.job.JobSwipe;
import com.jobmatcher.api.domain.job.SwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobSwipeRepository extends JpaRepository<JobSwipe, UUID> {

    Optional<JobSwipe> findByCandidateUsernameAndJobId(String candidateUsername, UUID jobId);

    List<JobSwipe> findByCandidateUsernameAndActionOrderByCreatedAtDesc(String candidateUsername, SwipeAction action);

    List<JobSwipe> findByCandidateUsernameOrderByCreatedAtDesc(String candidateUsername);
}
