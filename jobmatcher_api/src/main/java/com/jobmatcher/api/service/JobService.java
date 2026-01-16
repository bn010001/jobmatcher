package com.jobmatcher.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.api.domain.job.Job;
import com.jobmatcher.api.domain.job.JobStatus;
import com.jobmatcher.api.dto.JobCreateRequest;
import com.jobmatcher.api.dto.JobDTO;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.exception.NotFoundException;
import com.jobmatcher.api.repository.JobRepository;
import com.jobmatcher.api.dto.EmbedResponse;
import com.jobmatcher.api.service.ai.JobAiClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository repo;
    private final JobAiClient jobAiClient;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository repo, JobAiClient jobAiClient, ObjectMapper objectMapper) {
        this.repo = repo;
        this.jobAiClient = jobAiClient;
        this.objectMapper = objectMapper;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean canAccessAll() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DEV"));
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public JobDTO create(JobCreateRequest req) {
        if (req == null || req.title() == null || req.title().trim().isEmpty()) {
            throw new BadRequestException("title è obbligatorio");
        }

        String owner = currentUsername();

        JobStatus status = JobStatus.PUBLISHED;
        if (req.status() != null && !req.status().trim().isEmpty()) {
            try {
                status = JobStatus.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new BadRequestException("status non valido (DRAFT, PUBLISHED, ARCHIVED)");
            }
        }

        Job job = Job.builder()
                .ownerUsername(owner)
                .title(req.title().trim())
                .description(trimToNull(req.description()))
                .location(trimToNull(req.location()))
                .lat(req.lat())
                .lon(req.lon())
                .contractType(trimToNull(req.contractType()))
                .seniority(trimToNull(req.seniority()))
                .status(status)
                .applyUrl(trimToNull(req.applyUrl()))
                .build();

        if (status == JobStatus.PUBLISHED) {
            job.setPublishedAt(Instant.now());
        }
        if (status == JobStatus.ARCHIVED) {
            job.setArchivedAt(Instant.now());
        }

        Job saved = repo.save(job);

        // Se pubblicato: embedding (best-effort per demo; se vuoi hard-fail, vedi commento sotto)
        if (saved.getStatus() == JobStatus.PUBLISHED) {
            try {
                ensureEmbedded(saved);
                saved = repo.save(saved);
            } catch (Exception ignored) {
                // hard requirement? sostituisci con:
                // throw new BadRequestException("Impossibile calcolare embedding job (AI non disponibile)");
            }
        }

        return toDto(saved);
    }

    // Lista pubblica: per candidate mostra solo PUBLISHED
    public List<JobDTO> listPublished() {
        return repo.findByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED).stream()
                .map(this::toDto)
                .toList();
    }

    // Lista “mine”: per company/dev/admin
    public List<JobDTO> listMine() {
        String owner = currentUsername();
        if (canAccessAll()) {
            return repo.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return repo.findByOwnerUsernameOrderByCreatedAtDesc(owner).stream()
                .map(this::toDto)
                .toList();
    }

    // Public get: candidate vede solo PUBLISHED (production-ready)
    public JobDTO getById(UUID id) {
        Job job = repo.findById(id).orElseThrow(() -> new NotFoundException("Job non trovato"));

        if (!canAccessAll()) {
            boolean isCandidate = hasRole("CANDIDATE");
            String username = currentUsername();
            boolean isOwner = username != null && username.equals(job.getOwnerUsername());

            // candidate: solo PUBLISHED
            if (isCandidate && job.getStatus() != JobStatus.PUBLISHED) {
                throw new NotFoundException("Job non trovato");
            }

            // company non-owner: solo PUBLISHED (più “prod”)
            if (!isCandidate && !isOwner && job.getStatus() != JobStatus.PUBLISHED) {
                throw new NotFoundException("Job non trovato");
            }
        }

        return toDto(job);
    }

    public JobDTO getMineById(UUID id) {
        Job job = getJobForOwnerOrAdmin(id);
        return toDto(job);
    }

    public JobDTO updateStatus(UUID jobId, String newStatusRaw) {
        if (jobId == null) throw new BadRequestException("jobId mancante");
        if (newStatusRaw == null || newStatusRaw.trim().isEmpty()) throw new BadRequestException("status mancante");

        JobStatus newStatus;
        try {
            newStatus = JobStatus.valueOf(newStatusRaw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("status non valido (DRAFT, PUBLISHED, ARCHIVED)");
        }

        Job job = getJobForOwnerOrAdmin(jobId);

        JobStatus old = job.getStatus();
        if (old == newStatus) return toDto(job);

        job.setStatus(newStatus);

        if (newStatus == JobStatus.PUBLISHED && job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
        }
        if (newStatus == JobStatus.ARCHIVED) {
            job.setArchivedAt(Instant.now());
        }

        // Se va a PUBLISHED e manca embedding => embed
        if (newStatus == JobStatus.PUBLISHED) {
            try {
                ensureEmbedded(job);
            } catch (Exception ignored) {
                // hard requirement? throw new BadRequestException("Impossibile calcolare embedding job (AI non disponibile)");
            }
        }

        Job saved = repo.save(job);
        return toDto(saved);
    }

    private Job getJobForOwnerOrAdmin(UUID jobId) {
        String owner = currentUsername();
        if (canAccessAll()) {
            return repo.findById(jobId).orElseThrow(() -> new NotFoundException("Job non trovato"));
        }
        return repo.findByIdAndOwnerUsername(jobId, owner).orElseThrow(() -> new NotFoundException("Job non trovato"));
    }

    private void ensureEmbedded(Job job) {
        if (job.getEmbedding() != null && !job.getEmbedding().isNull()) return;

        String text = buildTextForEmbedding(job);
        EmbedResponse emb = jobAiClient.embedText(text);

        JsonNode json = objectMapper.valueToTree(emb.embedding());
        job.setEmbedding(json);
        job.setEmbeddingModel(emb.model_used());
        job.setEmbeddingUpdatedAt(Instant.now());
    }

    private String buildTextForEmbedding(Job job) {
        StringBuilder sb = new StringBuilder();
        if (job.getTitle() != null) sb.append(job.getTitle()).append("\n");
        if (job.getDescription() != null) sb.append(job.getDescription()).append("\n");
        if (job.getContractType() != null) sb.append("Contract: ").append(job.getContractType()).append("\n");
        if (job.getSeniority() != null) sb.append("Seniority: ").append(job.getSeniority()).append("\n");
        if (job.getLocation() != null) sb.append("Location: ").append(job.getLocation()).append("\n");
        return sb.toString().trim();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public JobDTO toDto(Job j) {
        boolean embedded = j.getEmbedding() != null && !j.getEmbedding().isNull();

        return new JobDTO(
                j.getId(),
                j.getOwnerUsername(),
                j.getTitle(),
                j.getDescription(),
                j.getLocation(),
                j.getLat(),
                j.getLon(),
                j.getContractType(),
                j.getSeniority(),
                j.getStatus() != null ? j.getStatus().name() : null,
                j.getPublishedAt(),
                j.getArchivedAt(),
                j.getApplyUrl(),
                embedded,
                j.getEmbeddingModel(),
                j.getEmbeddingUpdatedAt(),
                j.getCreatedAt(),
                j.getUpdatedAt()
        );
    }
}