package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class OpenAiChequeOcrService {

    private static final String MODEL = "gemini-2.5-flash";
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

        String responseText = geminiClient.generateTextFromImage(MODEL, OCR_PROMPT, imagePath, detectMimeType(imagePath), 512);
        return normalizeJson(responseText);
    }

    public ChequeOcrResult extractCheque(Path imagePath) throws Exception {
        return mapper.readValue(extractChequeJson(imagePath), ChequeOcrResult.class);
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
