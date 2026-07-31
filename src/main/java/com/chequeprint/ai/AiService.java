package com.chequeprint.ai;

import com.chequeprint.util.Session;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Client service routing AI requests through Spring Boot Backend REST API (/api/ai/ask).
 */
public class AiService {

    private static final Logger LOGGER = Logger.getLogger(AiService.class.getName());
    private static final String BACKEND_AI_URL = "http://localhost:8081/api/ai/ask";
    private static final AiService INSTANCE = new AiService();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService() {}

    public static AiService getInstance() {
        return INSTANCE;
    }

    public String askAI(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Error: Prompt cannot be empty.";
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of("prompt", prompt));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_AI_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            String token = SessionManager.getInstance().getToken();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("success").asBoolean(true)) {
                    return root.path("response").asText();
                } else {
                    return "⚠️ " + root.path("error").asText("Unknown backend error");
                }
            } else {
                return "⚠️ Unable to connect to AI Service (HTTP " + response.statusCode() + "). Please check your server connection.";
            }
        } catch (java.net.http.HttpTimeoutException e) {
            return "⚠️ Request Timed Out. Please check your internet connection and try again.";
        } catch (java.net.ConnectException e) {
            return "⚠️ Unable to connect to the backend server. Please make sure the REST API service is running.";
        } catch (Exception e) {
            LOGGER.severe("Backend AI API Communication Error: " + e.getMessage());
            return "⚠️ AI Error: " + (e.getMessage() != null ? e.getMessage() : "Connection failed.");
        }
    }

    public String extractChequeData(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        return askAI("Extract cheque information (Payee, Amount, Date) from: " + text);
    }
}
