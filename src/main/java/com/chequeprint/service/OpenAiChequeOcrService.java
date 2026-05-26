package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiChequeOcrService {

    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final String MODEL = "gpt-5.2";
    private static final String OCR_PROMPT = """
            Extract name, amount, date from cheque image text.

            Rules:
            - Return only valid JSON
            - Do not explain anything
            - If a value is missing or unreadable, keep it ""
            - name means payee name
            - amount must be numeric text only when possible
            - date must be ISO format yyyy-MM-dd when possible

            Output format:
            {
              "name": "",
              "amount": "",
              "date": ""
            }
            """;

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiChequeOcrService() {
        this(HttpClient.newHttpClient());
    }

    OpenAiChequeOcrService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String extractChequeJson(Path imagePath) throws Exception {
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            return emptyResult();
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set.");
        }

        String body = buildRequestBody(imagePath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESPONSES_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI OCR request failed: HTTP "
                    + response.statusCode() + " - " + response.body());
        }

        return normalizeJson(extractOutputText(response.body()));
    }

    public ChequeOcrResult extractCheque(Path imagePath) throws Exception {
        return mapper.readValue(extractChequeJson(imagePath), ChequeOcrResult.class);
    }

    private String buildRequestBody(Path imagePath) throws Exception {
        String dataUrl = "data:" + detectMimeType(imagePath) + ";base64,"
                + Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));

        Map<String, Object> imageContent = new LinkedHashMap<>();
        imageContent.put("type", "input_image");
        imageContent.put("image_url", dataUrl);

        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "input_text");
        textContent.put("text", OCR_PROMPT);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", java.util.List.of(textContent, imageContent));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", MODEL);
        payload.put("input", java.util.List.of(message));

        return mapper.writeValueAsString(payload);
    }

    private String extractOutputText(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        StringBuilder text = new StringBuilder();

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        if (part.hasNonNull("text")) {
                            text.append(part.path("text").asText());
                        }
                    }
                }
            }
        }

        return text.toString().trim();
    }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return emptyResult();
        }

        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return emptyResult();
    }

    private String detectMimeType(Path imagePath) throws Exception {
        String mime = Files.probeContentType(imagePath);
        if (mime != null && mime.startsWith("image/")) {
            return mime;
        }

        String file = imagePath.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (file.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    private String emptyResult() {
        return """
                {"name":"","amount":"","date":""}
                """.trim();
    }

    public static class ChequeOcrResult {
        private String name = "";
        private String amount = "";
        private String date = "";

        public String getName() {
            return name == null ? "" : name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAmount() {
            return amount == null ? "" : amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDate() {
            return date == null ? "" : date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}
