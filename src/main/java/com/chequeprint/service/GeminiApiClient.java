package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String DEFAULT_MODEL = "gemini-pro";
    private static final int MAX_RETRIES = 3;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int REQUEST_TIMEOUT_MS = 30000;
    private static final int MAX_PROMPT_LENGTH = 4096;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 256;

    private final ObjectMapper mapper;
    private final Random random;

    public GeminiApiClient() {
        this(new ObjectMapper(), new Random());
    }

    GeminiApiClient(ObjectMapper mapper, Random random) {
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
        if ("MOCK_KEY".equals(apiKey)) {
            Thread.sleep(1500);
            return "This is a simulated AI response.\n(To enable real AI, set the GEMINI_API_KEY environment variable and restart the application.)\n\nYou said: " + text;
        }

        String requestBody = buildPayload(text, maxOutputTokens);
        String url = buildUrl(model, apiKey);

        return executeWithRetry(url, requestBody, apiKey);
    }

    public String generateTextFromImage(String model, String text, Path imagePath, String mimeType, int maxOutputTokens)
            throws Exception {
        validateModel(model);
        validateText(text);
        validateImage(imagePath, mimeType);
        validateMaxOutputTokens(maxOutputTokens);

        String apiKey = getApiKey();
        if ("MOCK_KEY".equals(apiKey)) {
            Thread.sleep(2000);
            return "This is a simulated AI response for the scanned image.\n(To enable real OCR, set the GEMINI_API_KEY environment variable.)\n\nImage analyzed successfully.";
        }

        String imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        String requestBody = buildImagePayload(text, imageBase64, mimeType, maxOutputTokens);
        String url = buildUrl(model, apiKey);

        return executeWithRetry(url, requestBody, apiKey);
    }

    private String executeWithRetry(String urlStr, String requestBody, String apiKey) throws Exception {
        int attempt = 0;
        while (true) {
            attempt++;
            final int currentAttempt = attempt;
            try {
                LOGGER.fine(() -> "Gemini request attempt " + currentAttempt + " to " + urlStr);
                return executeRequest(urlStr, requestBody, apiKey, attempt);
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

    private String executeRequest(String urlStr, String requestBody, String apiKey, int attempt) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(REQUEST_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int statusCode = connection.getResponseCode();
            LOGGER.fine(() -> "Gemini HTTP " + statusCode + " on attempt " + attempt);

            if (statusCode / 100 != 2) {
                String errorBody = readStream(connection.getErrorStream());
                handleHttpError(statusCode, errorBody, attempt);
            }

            String responseBody = readStream(connection.getInputStream());
            LOGGER.finer(() -> "Gemini response body: " + responseBody);

            return parseResponse(responseBody);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private void handleHttpError(int status, String body, int attempt) throws Exception {
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
            return "MOCK_KEY";
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
        if (tokens <= 0 || tokens > 2048) {
            throw new IllegalArgumentException("maxOutputTokens must be between 1 and 2048.");
        }
    }
}
