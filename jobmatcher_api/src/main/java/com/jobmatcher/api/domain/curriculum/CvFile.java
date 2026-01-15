package com.jobmatcher.api.domain.curriculum;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cv_file")
public class CvFile {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false, length = 1024)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 1024)
    private String storagePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode analysisJson;

    private Instant analyzedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CvProcessingStatus status;

    @Column(columnDefinition = "text")
    private String errorMessage;
}