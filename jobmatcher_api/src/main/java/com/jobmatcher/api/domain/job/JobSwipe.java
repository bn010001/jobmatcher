package com.jobmatcher.api.domain.job;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job_swipe",
       uniqueConstraints = @UniqueConstraint(name = "uk_job_swipe_candidate_job", columnNames = {"candidate_username", "job_id"}))
public class JobSwipe {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "candidate_username", nullable = false, length = 150)
    private String candidateUsername;

    @Column(name = "job_id", nullable = false, columnDefinition = "uuid")
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SwipeAction action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
