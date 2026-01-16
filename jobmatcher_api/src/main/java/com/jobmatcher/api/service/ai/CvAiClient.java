package com.jobmatcher.api.service.ai;

import com.jobmatcher.api.dto.CvParseResponse;
import com.jobmatcher.api.dto.EmbedResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class CvAiClient {
    private final WebClient webClient;
    private record EmbedTextBody(String text) {}

    public CvAiClient(@Value("${jobmatcher.ai.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public CvParseResponse parseText(String text) {
        return webClient.post()
                .uri("/cv/parse-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TextBody(text))
                .retrieve()
                .bodyToMono(CvParseResponse.class)
                .block();
    }

    private record TextBody(String text) {}

    private Mono<CvParseResponse> postParseFile(MultipartBodyBuilder builder) {
        return webClient.post()
                .uri("/cv/parse-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(status -> status.isError(), resp ->
                        resp.bodyToMono(String.class).flatMap(body ->
                                Mono.error(new RuntimeException("AI " + resp.statusCode() + ": " + body))
                        )
                )
                .bodyToMono(CvParseResponse.class);
    }

    public CvParseResponse parseFile(MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        ByteArrayResource resource;
        try {
            resource = new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv";
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Errore lettura file", e);
        }

        String ct = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/octet-stream"
                : file.getContentType();

        builder.part("file", resource)
                .filename(resource.getFilename())           // <-- IMPORTANTISSIMO
                .contentType(MediaType.parseMediaType(ct));

        return postParseFile(builder).block();
    }

    public CvParseResponse parseResource(Resource resource, String filename, String contentType) {

        byte[] bytes;
        try (var is = resource.getInputStream()) {
            bytes = is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Errore lettura Resource CV", e);
        }

        String safeFilename = (filename == null || filename.isBlank()) ? "cv" : filename;
        String ct = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;

        ByteArrayResource bar = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return safeFilename;
            }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", bar)
                .filename(safeFilename)
                .contentType(MediaType.parseMediaType(ct));

        return webClient.post()
                .uri("/cv/parse-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CvParseResponse.class)
                .block();
    }

    public EmbedResponse embedText(String text) {
        return webClient.post()
                .uri("/jobs/embed-text") // endpoint python che creeremo
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new EmbedTextBody(text))
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .block();
    }
}

