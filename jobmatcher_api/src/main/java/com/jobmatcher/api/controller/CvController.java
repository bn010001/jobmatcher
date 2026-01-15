package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.CvAnalysisDTO;
import com.jobmatcher.api.dto.CvFileDTO;
import com.jobmatcher.api.dto.CvParseResponse;
import com.jobmatcher.api.dto.CvParseTextRequest;
import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.service.CvService;
import com.jobmatcher.api.service.ai.CvAiClient;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv")
public class CvController {

    private final CvService cvService;
    private final CvAiClient cvAiClient;

    public CvController(CvService cvService, CvAiClient cvAiClient) {
        this.cvService = cvService;
        this.cvAiClient = cvAiClient;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvFileDTO> upload(@RequestPart("file") MultipartFile file) {
        CvFile saved = cvService.upload(file);
        CvFileDTO dto = cvService.toDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public List<CvFileDTO> listMine() {
        return cvService.listMine().stream().map(cvService::toDto).toList();
    }

    @GetMapping("/{cvId}")
    public CvFileDTO getMine(@PathVariable UUID cvId) {
        return cvService.toDto(cvService.getMine(cvId));
    }

   @PostMapping("/{cvId}/analyze")
    public CvParseResponse analyze(
            @PathVariable UUID cvId,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return cvService.analyze(cvId, force);
    }

    @GetMapping("/{cvId}/analysis")
    public CvAnalysisDTO getAnalysis(@PathVariable UUID cvId) {
        return cvService.getAnalysisMine(cvId);
    }

    @GetMapping("/{cvId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID cvId) {
        CvFile cv = cvService.getMine(cvId);
        Resource res = cvService.downloadMine(cvId);

        String filename = (cv.getOriginalFilename() == null || cv.getOriginalFilename().isBlank())
                ? "cv"
                : cv.getOriginalFilename();

        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (cv.getContentType() != null && !cv.getContentType().isBlank()) {
            try { mt = MediaType.parseMediaType(cv.getContentType()); } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(res);
    }

    // =========================
    // DEBUG endpoints (solo dev)
    // =========================
    @Profile("dev")
    @RestController
    @RequestMapping("/api/cv/debug")
    static class CvDebugController {

        private final CvAiClient cvAiClient;

        CvDebugController(CvAiClient cvAiClient) {
            this.cvAiClient = cvAiClient;
        }

        @PostMapping("/parse-text")
        public CvParseResponse parseText(@Valid @RequestBody CvParseTextRequest req) {
            return cvAiClient.parseText(req.getText().trim());
        }

        @PostMapping(value = "/parse-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public CvParseResponse parseFile(@RequestPart("file") MultipartFile file) {
            return cvAiClient.parseFile(file);
        }
    }
}
