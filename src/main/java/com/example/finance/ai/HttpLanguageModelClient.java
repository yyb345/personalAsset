package com.example.finance.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Supports providers exposing the chat/completions protocol. Secrets stay server-side. */
@Service
public class HttpLanguageModelClient implements LanguageModelClient {
    private final RestClient client;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final String thinkingMode;

    public HttpLanguageModelClient(@Value("${ai.endpoint:}") String endpoint,
                                   @Value("${ai.api-key:}") String apiKey,
                                   @Value("${ai.model:}") String model,
                                   @Value("${ai.thinking-mode:}") String thinkingMode) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.thinkingMode = thinkingMode;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(120000);
        client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String complete(String instruction, String content) {
        if (endpoint.isBlank() || apiKey.isBlank() || model.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "请先配置 DEEPSEEK_API_KEY（或 AI_API_KEY），并检查 AI_ENDPOINT 和 AI_MODEL");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                Map.of("role", "system", "content", instruction),
                Map.of("role", "user", "content", content)));
            body.put("max_tokens", 1600);
            if (!thinkingMode.isBlank()) {
                body.put("thinking", Map.of("type", thinkingMode));
            }
            JsonNode response = client.post().uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);
            String text = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            if (text.isBlank() || "length".equals(response.path("choices").path(0).path("finish_reason").asText())) {
                throw new IllegalStateException("Empty or incomplete model response");
            }
            return text.trim();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 摘要生成失败，请稍后重试");
        }
    }
}
