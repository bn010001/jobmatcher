package com.jobmatcher.api.controller;

import com.jobmatcher.api.dto.CvParseResponse;
import com.jobmatcher.api.dto.CvParseTextRequest;
import com.jobmatcher.api.service.AiCvService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/cv")
public class CvController {

    private final AiCvService aiCvService;

    public CvController(AiCvService aiCvService) {
        this.aiCvService = aiCvService;
    }

    @PostMapping("/parse-text")
    public Mono<CvParseResponse> parseText(@Valid @RequestBody CvParseTextRequest req) {
        return aiCvService.parseText(req.getText().trim());
    }

    @PostMapping(value = "/parse-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CvParseResponse> parseFile(@RequestPart("file") MultipartFile file) {
        return aiCvService.parseFile(file);
    }
}
