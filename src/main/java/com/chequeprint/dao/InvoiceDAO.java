package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.Invoice;
import com.chequeprint.util.Session;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private static final String API_INVOICES = ApiConfig.BASE_URL + "/api/invoices";
    private static volatile long lastErrorLogTime = 0;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public InvoiceDAO() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void addAuthToken(HttpRequest.Builder builder) {
        String authHeader = Session.getAuthorizationHeader();
        if (authHeader != null && !authHeader.isBlank()) {
            builder.header("Authorization", authHeader);
        }
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                if (i == maxRetries - 1) throw e;
                Thread.sleep(500L * (i + 1));
            }
        }
        return null;
    }

    public List<Invoice> findAll() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_INVOICES))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<List<Invoice>>() {});
            }
        } catch (Exception ex) {
            logError("InvoiceDAO findAll error: " + ex.getMessage());
        }
        return new ArrayList<>();
    }

    public boolean insert(Invoice inv) {
        try {
            String json = objectMapper.writeValueAsString(inv);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_INVOICES))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("InvoiceDAO insert error: " + ex.getMessage());
            return false;
        }
    }

    public boolean update(Invoice inv) {
        if (inv == null || inv.getId() <= 0) return false;
        try {
            String json = objectMapper.writeValueAsString(inv);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_INVOICES + "/" + inv.getId()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("InvoiceDAO update error: " + ex.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(API_INVOICES + "/" + id));
            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logError("InvoiceDAO delete error: " + ex.getMessage());
            return false;
        }
    }

    public int countTotal() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_INVOICES + "/count"))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            if (response.statusCode() == 200) {
                return Integer.parseInt(response.body().trim());
            }
        } catch (Exception ex) {
            logError("InvoiceDAO countTotal error: " + ex.getMessage());
        }
        return 0;
    }

    private void logError(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > 5000) {
            lastErrorLogTime = now;
            System.err.println(message);
        }
    }
}