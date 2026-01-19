package com.jobmatcher.api.domain.job;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobmatcher.api.domain.job.JobStatus;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job")
public class Job {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "owner_username", nullable = false, length = 150)
    private String ownerUsername;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "contract_type", length = 80)
    private String contractType;

    @Column(length = 80)
    private String seniority;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 32)
    private JobStatus status = JobStatus.PUBLISHED;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode embedding;

    @Column(name="embedding_model", length=120)
    private String embeddingModel;

    @Column(name="embedding_updated_at")
    private Instant embeddingUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

