package com.jobmatcher.api.service;

import com.jobmatcher.api.config.CvProperties;
import com.jobmatcher.api.domain.candidate.CandidateProfile;
import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;
import com.jobmatcher.api.repository.CvFileRepository;
import com.jobmatcher.api.service.storage.StorageService;
import com.jobmatcher.api.dto.CvAnalysisDTO;
import com.jobmatcher.api.dto.CvFileDTO;
import com.jobmatcher.api.service.CandidateProfileService;
import com.jobmatcher.api.dto.CvParseResponse;
import com.jobmatcher.api.service.ai.CvAiClient;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.exception.NotFoundException;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.client.RestClientException;

import com.jobmatcher.api.exception.ConflictException;
import com.jobmatcher.api.exception.ServiceUnavailableException;


import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

@Service
public class CvService {

    private final CvFileRepository repo;
    private final StorageService storage;
    private final CandidateProfileService candidateProfileService;
    private final CvProperties cvProps;
    private final CvAiClient cvAiClient;
    private final ObjectMapper objectMapper;

    public CvService(CvFileRepository repo, StorageService storage, CandidateProfileService candidateProfileService, CvProperties cvProps, CvAiClient cvAiClient, ObjectMapper objectMapper) {
        this.repo = repo;
        this.storage = storage;
        this.candidateProfileService = candidateProfileService;
        this.cvProps = cvProps;
        this.cvAiClient = cvAiClient;
        this.objectMapper = objectMapper;
    }

    private boolean canAccessAll() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DEV"));
    }

    public CvFile upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File mancante");
    
        String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("cv");
        String ext = extension(original);
    
        if (file.getSize() > cvProps.getMaxSizeBytes()) {
            throw new BadRequestException("File troppo grande: " + file.getSize());
        }
    
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
    
        boolean allowedByType = cvProps.getAllowedContentTypes().isEmpty()
                || cvProps.getAllowedContentTypes().contains(contentType);

        boolean allowedByExt = Set.of("pdf", "docx").contains(ext);

        boolean allowed = allowedByType || allowedByExt;
        if (!allowed) {
            throw new BadRequestException("Tipo file non consentito: " + contentType + " / ." + ext);
        }

    
        String storedFilename = UUID.randomUUID() + (ext.isBlank() ? "" : ("." + ext));
    
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (Exception e) { throw new IllegalStateException("Errore lettura file", e); }
    
       storage.save(bytes, storedFilename);

        CvFile cv = new CvFile();
        cv.setOwnerUsername(owner);
        cv.setOriginalFilename(original);
        cv.setContentType(contentType);
        cv.setSizeBytes(file.getSize());
        cv.setStoragePath(storedFilename);
        cv.setStatus(CvProcessingStatus.UPLOADED);
        cv.setErrorMessage(null);

        try {
            return repo.save(cv);
        } catch (Exception e) {
            // evita file orfano se il DB fallisce
            try { storage.delete(storedFilename); } catch (Exception ignored) {}
            throw e;
        }
    }

    public CvParseResponse analyze(UUID cvId, boolean force) {
        CvFile cv = loadCvForCurrentUser(cvId);

        // best-effort: evita doppio click che lancia 2 parse in parallelo
        if (!force && cv.getStatus() == CvProcessingStatus.PARSING) {
            throw new ConflictException("CV già in analisi");
        }

        boolean alreadyParsed = cv.getStatus() == CvProcessingStatus.PARSED
                && cv.getAnalyzedAt() != null
                && cv.getAnalysisJson() != null;

        if (!force && alreadyParsed) {
            try {
                return objectMapper.treeToValue(cv.getAnalysisJson(), CvParseResponse.class);
            } catch (Exception e) {
                throw new IllegalStateException("Analysis salvata non convertibile in CvParseResponse", e);
            }
        }

        // set PARSING e salva subito (così la UI può vedere lo stato)
        cv.setStatus(CvProcessingStatus.PARSING);
        cv.setErrorMessage(null);
        try {
            repo.save(cv);
        } catch (ObjectOptimisticLockingFailureException e) {
            // un’altra richiesta ha già aggiornato lo stesso CV
            throw new ConflictException("CV già in analisi");
        }


        try {
            Resource res = storage.loadAsResource(cv.getStoragePath());

            CvParseResponse response = cvAiClient.parseResource(
                    res,
                    cv.getOriginalFilename(),
                    cv.getContentType()
            );

            cv.setAnalysisJson(objectMapper.valueToTree(response));
            cv.setAnalyzedAt(Instant.now());
            cv.setStatus(CvProcessingStatus.PARSED);
            cv.setErrorMessage(null);

            repo.save(cv);

            candidateProfileService.setActiveCv(cv.getOwnerUsername(), cv.getId());

            return response;

        } catch (Exception e) {
            // best effort: segna FAILED
            try {
                cv.setStatus(CvProcessingStatus.FAILED);
                cv.setErrorMessage(truncate(e.getMessage(), 500));
                repo.save(cv);
            } catch (Exception ignored) {}
        
            // errore “pulito” verso API
            if (e instanceof RestClientException) {
                throw new ServiceUnavailableException("Servizio AI non disponibile");
            }
        
            throw new BadRequestException("Impossibile analizzare CV: " + truncate(e.getMessage(), 200));
        }

    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }

    private CvFile loadCvForCurrentUser(UUID cvId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (canAccessAll()) {
            return repo.findById(cvId).orElseThrow(() -> new NotFoundException("CV non trovato"));
        }

        return repo.findByIdAndOwnerUsername(cvId, username)
                .orElseThrow(() -> new NotFoundException("CV non trovato"));
    }


    public Resource downloadMine(UUID cvId) {
        CvFile cv = loadCvForCurrentUser(cvId);
        return storage.loadAsResource(cv.getStoragePath());
    }

    public CvFile getMine(UUID cvId) {
        return loadCvForCurrentUser(cvId);
    }

    public List<CvFile> listMine() {
        if (canAccessAll()) {
            return repo.findAllByOrderByUploadedAtDesc();
        }
        String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        return repo.findByOwnerUsernameOrderByUploadedAtDesc(owner);
    }

    public CvAnalysisDTO getAnalysisMine(UUID cvId) {
        CvFile cv = loadCvForCurrentUser(cvId);
        return new CvAnalysisDTO(
                cv.getAnalyzedAt(),
                cv.getAnalysisJson(),
                cv.getStatus() != null ? cv.getStatus().name() : null,
                cv.getErrorMessage()
        );
    }


    private String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return "";
        return filename.substring(i + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public CvFileDTO toDto(CvFile cv) {
        boolean embedded = false;
        String embeddingModel = null;
    
        if (cv.getAnalysisJson() != null) {
            var emb = cv.getAnalysisJson().get("embedding");
            embedded = emb != null && emb.isArray() && emb.size() > 0;
        
            var model = cv.getAnalysisJson().get("model_used");
            if (model != null && model.isTextual()) embeddingModel = model.asText();
        }
    
        return new CvFileDTO(
                cv.getId(),
                cv.getOriginalFilename(),
                cv.getContentType(),
                cv.getSizeBytes(),
                cv.getUploadedAt(),
                cv.getAnalyzedAt(),
                cv.getStatus() != null ? cv.getStatus().name() : null,
                cv.getErrorMessage(),
                embedded,
                embeddingModel
        );
    }

}
