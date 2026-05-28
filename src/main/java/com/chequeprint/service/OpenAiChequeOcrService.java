package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiChequeOcrService {

    private static final String MODEL = "gemini-1.5";
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

    private final GeminiApiClient geminiClient = new GeminiApiClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiChequeOcrService() {
    }

    public String extractChequeJson(Path imagePath) throws Exception {
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            return emptyResult();
        }

        List<Map<String, Object>> messages = List.of(buildOcrMessage(imagePath));
        String responseText = geminiClient.generateText(MODEL, messages, Map.of(
                "temperature", 0.0,
                "max_output_tokens", 1024));

        return normalizeJson(responseText);
    }

    public ChequeOcrResult extractCheque(Path imagePath) throws Exception {
        return mapper.readValue(extractChequeJson(imagePath), ChequeOcrResult.class);
    }

    private Map<String, Object> buildOcrMessage(Path imagePath) throws Exception {
        String dataUrl = "data:" + detectMimeType(imagePath) + ";base64,"
                + Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));

        Map<String, Object> imageContent = new LinkedHashMap<>();
        imageContent.put("type", "input_image");
        imageContent.put("image_url", dataUrl);

        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "input_text");
        textContent.put("text", OCR_PROMPT);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("author", "user");
        message.put("content", List.of(textContent, imageContent));
        return message;
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
