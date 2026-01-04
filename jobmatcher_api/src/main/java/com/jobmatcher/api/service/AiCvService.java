package com.jobmatcher.api.service;

import com.jobmatcher.api.dto.CvParseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;

@Service
public class AiCvService {

    private final WebClient webClient;

    public AiCvService(@Value("${jobmatcher.ai.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<CvParseResponse> parseText(String text) {
        return webClient.post()
                .uri("/cv/parse-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TextBody(text))
                .retrieve()
                .bodyToMono(CvParseResponse.class);
    }

    // body interno per la richiesta al microservizio AI
    private record TextBody(String text) {}

    public Mono<CvParseResponse> parseFile(MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        ByteArrayResource resource;
        try {
            resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Errore lettura file", e));
        }

        builder.part("file", resource)
                .contentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "application/octet-stream"));

        return webClient.post()
                .uri("/cv/parse-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CvParseResponse.class);
    }

}
