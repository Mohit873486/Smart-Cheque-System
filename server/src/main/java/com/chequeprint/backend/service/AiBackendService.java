package com.chequeprint.backend.service;

import com.chequeprint.backend.dto.AiPromptResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiBackendService {

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiPromptResponse generateAiResponse(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return AiPromptResponse.fail("Prompt text cannot be empty.");
        }

        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            Map<String, Object> payload = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return AiPromptResponse.ok(textNode.asText());
                    }
                }
                return AiPromptResponse.ok("AI response generated successfully.");
            } else {
                String body = response.body();
                if (response.statusCode() == 400 || response.statusCode() == 401 || (body != null && body.contains("API_KEY_INVALID"))) {
                    return AiPromptResponse.fail("Invalid AI API key configured. Please verify your Gemini API key settings.");
                } else if (response.statusCode() == 429) {
                    return AiPromptResponse.fail("AI rate limit exceeded. Please wait a moment and try again.");
                } else if (response.statusCode() >= 500) {
                    return AiPromptResponse.fail("AI service is temporarily unavailable (HTTP " + response.statusCode() + "). Please try again later.");
                } else {
                    return AiPromptResponse.fail("AI Service Error (HTTP " + response.statusCode() + "). Please try again.");
                }
            }
        } catch (java.net.http.HttpTimeoutException ex) {
            return AiPromptResponse.fail("AI Request Timed Out. Please check your internet connection and try again.");
        } catch (java.net.ConnectException ex) {
            return AiPromptResponse.fail("Unable to connect to AI service. Please check your internet connection.");
        } catch (Exception ex) {
            return AiPromptResponse.fail("AI Service Error: " + ex.getMessage());
        }
    }
}
