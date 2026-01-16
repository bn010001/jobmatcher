package com.jobmatcher.api.service.ai;

import com.jobmatcher.api.dto.EmbedResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class JobAiClient {

    private final WebClient webClient;

    public JobAiClient(@Value("${jobmatcher.ai.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    private record TextBody(String text) {}

    public EmbedResponse embedText(String text) {
        return webClient.post()
                .uri("/job/embed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TextBody(text))
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .block();
    }
}
