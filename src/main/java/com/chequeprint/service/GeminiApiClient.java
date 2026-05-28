package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeminiApiClient {

    private static final Logger LOGGER = Logger.getLogger(GeminiApiClient.class.getName());
    private static final String GEMINI_API_KEY_ENV = "GEMINI_API_KEY";
    private static final String API_ENDPOINT_FORMAT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final int MAX_RETRIES = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_PROMPT_LENGTH = 4096;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 128;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Random random;

    public GeminiApiClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build(), new ObjectMapper(), new Random());
    }

    GeminiApiClient(HttpClient httpClient, ObjectMapper mapper, Random random) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.random = random;
    }

    public String generateText(String text) throws Exception {
        return generateText(DEFAULT_MODEL, text, DEFAULT_MAX_OUTPUT_TOKENS);
    }

    public String generateText(String model, String text) throws Exception {
        return generateText(model, text, DEFAULT_MAX_OUTPUT_TOKENS);
    }

    public String generateText(String model, String text, int maxOutputTokens) throws Exception {
        validateModel(model);
        validateText(text);
        validateMaxOutputTokens(maxOutputTokens);

        String apiKey = getApiKey();
        String requestBody = buildPayload(text, maxOutputTokens);
        String url = buildUrl(model, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return executeWithRetry(request);
    }

    public String generateTextFromImage(String model, String text, Path imagePath, String mimeType, int maxOutputTokens)
            throws Exception {
        validateModel(model);
        validateText(text);
        validateImage(imagePath, mimeType);
        validateMaxOutputTokens(maxOutputTokens);

        String apiKey = getApiKey();
        String imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        String requestBody = buildImagePayload(text, imageBase64, mimeType, maxOutputTokens);
        String url = buildUrl(model, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return executeWithRetry(request);
    }

    private String executeWithRetry(HttpRequest request) throws Exception {
        int attempt = 0;
        while (true) {
            attempt++;
            final int currentAttempt = attempt;
            try {
                LOGGER.fine(() -> "Gemini request attempt " + currentAttempt + " to " + request.uri());
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logResponse(response, attempt);

                if (response.statusCode() / 100 != 2) {
                    handleHttpError(response, attempt);
                }

                return parseResponse(response.body());
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (attempt >= MAX_RETRIES || !isRetryableException(ex)) {
                    throw new IllegalStateException("Gemini request failed after " + attempt + " attempts", ex);
                }
                long backoff = computeBackoffMillis(attempt);
                LOGGER.log(Level.WARNING, "Gemini request failed on attempt {0}, retrying after {1}ms: {2}",
                        new Object[]{attempt, backoff, ex.getMessage()});
                Thread.sleep(backoff);
            }
        }
    }

    private void handleHttpError(HttpResponse<String> response, int attempt) throws Exception {
        String body = response.body();
        int status = response.statusCode();
        LOGGER.log(Level.WARNING, "Gemini returned HTTP {0} on attempt {1}: {2}", new Object[]{status, attempt, body});

        if (attempt >= MAX_RETRIES || status < 500) {
            throw new IllegalStateException("Gemini request failed: HTTP " + status + " - " + body);
        }
    }

    private String parseResponse(String responseBody) throws Exception {
        Objects.requireNonNull(responseBody, "Gemini response body must not be null");

        JsonNode root = mapper.readTree(responseBody);
        JsonNode candidate = root.path("candidates").path(0);
        if (candidate.isMissingNode() || candidate.isNull()) {
            throw new IllegalStateException("No candidates found in Gemini response.");
        }

        JsonNode content = candidate.path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("No content found in Gemini candidate.");
        }

        JsonNode part = content.path("parts").path(0);
        if (part.isMissingNode() || part.isNull() || !part.hasNonNull("text")) {
            throw new IllegalStateException("No text part found in Gemini response candidate.");
        }

        String text = part.path("text").asText();
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini response text is empty.");
        }
        return text.trim();
    }

    private void logResponse(HttpResponse<String> response, int attempt) {
        LOGGER.fine(() -> "Gemini HTTP " + response.statusCode() + " on attempt " + attempt);
        LOGGER.finer(() -> "Gemini response body: " + response.body());
    }

    private boolean isRetryableException(Exception ex) {
        return ex instanceof IOException;
    }

    private long computeBackoffMillis(int attempt) {
        long base = 200L * (1L << (attempt - 1));
        return base + random.nextInt(200);
    }

    private String buildUrl(String model, String apiKey) {
        return String.format(API_ENDPOINT_FORMAT, model) + "?key=" + apiKey;
    }

    private String buildPayload(String text, int maxOutputTokens) throws Exception {
        Map<String, Object> part = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.0,
                "maxOutputTokens", maxOutputTokens
        );
        Map<String, Object> payload = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );
        return mapper.writeValueAsString(payload);
    }

    private String buildImagePayload(String text, String imageBase64, String mimeType, int maxOutputTokens) throws Exception {
        Map<String, Object> imagePart = Map.of(
                "inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", imageBase64
                )
        );
        Map<String, Object> textPart = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(imagePart, textPart));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.0,
                "maxOutputTokens", maxOutputTokens
        );
        Map<String, Object> payload = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );
        return mapper.writeValueAsString(payload);
    }

    private String getApiKey() {
        String apiKey = System.getenv(GEMINI_API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(GEMINI_API_KEY_ENV + " environment variable is not set.");
        }
        return apiKey.trim();
    }

    private void validateModel(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Gemini model must be provided.");
        }
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Prompt text must not be blank.");
        }
        if (text.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Prompt text exceeds max length of " + MAX_PROMPT_LENGTH + " characters.");
        }
    }

    private void validateImage(Path imagePath, String mimeType) {
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            throw new IllegalArgumentException("Image file must exist.");
        }
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Image MIME type must be valid.");
        }
    }

    private void validateMaxOutputTokens(int tokens) {
        if (tokens <= 0 || tokens > 512) {
            throw new IllegalArgumentException("maxOutputTokens must be between 1 and 512.");
        }
    }
}
