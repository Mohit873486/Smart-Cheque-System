package com.chequeprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chequeprint.model.Cheque;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

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
    private final ChequeService chequeService = new ChequeService();
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

    public OcrSaveResult extractAndSaveCheque(Path imagePath) throws Exception {
        ChequeOcrResult result = extractCheque(imagePath);
        BigDecimal amount = parseAmount(result.getAmount());

        if (result.getName().isBlank() || amount == null) {
            return new OcrSaveResult(false, "OCR completed, but payee name or amount is missing.", result, null);
        }

        Cheque cheque = new Cheque(null, result.getName().trim(), amount, 1, parseDateOrToday(result.getDate()));
        chequeService.save(cheque);
        return new OcrSaveResult(true, "OCR cheque saved: " + cheque.getChequeNo(), result, cheque);
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

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.replaceAll("[^0-9.\\-]", "");
        if (normalized.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDateOrToday(String raw) {
        LocalDate parsed = parseDate(raw);
        return parsed != null ? parsed : LocalDate.now();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("d/M/uuuu")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 8) {
            try {
                return LocalDate.parse(digits, DateTimeFormatter.ofPattern("ddMMuuuu"));
            } catch (DateTimeParseException ignored) {
            }
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if ("today".equals(lower)) {
            return LocalDate.now();
        }
        if ("tomorrow".equals(lower)) {
            return LocalDate.now().plusDays(1);
        }
        return null;
    }

    public static class OcrSaveResult {
        private final boolean saved;
        private final String message;
        private final ChequeOcrResult ocrResult;
        private final Cheque cheque;

        public OcrSaveResult(boolean saved, String message, ChequeOcrResult ocrResult, Cheque cheque) {
            this.saved = saved;
            this.message = message;
            this.ocrResult = ocrResult;
            this.cheque = cheque;
        }

        public boolean isSaved() {
            return saved;
        }

        public String getMessage() {
            return message;
        }

        public ChequeOcrResult getOcrResult() {
            return ocrResult;
        }

        public Cheque getCheque() {
            return cheque;
        }
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
