package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.Bank;
import com.chequeprint.util.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for Bank & Template persistence operations.
 * Isolates low-level REST API and database communications from business services.
 */
public class BankDAO {

    private static final String API_BANKS = ApiConfig.BASE_URL + "/api/banks";
    private static final String API_TEMPLATES = ApiConfig.BASE_URL + "/api/templates";
    private static final String API_TEMPLATE_FIELDS = ApiConfig.BASE_URL + "/api/template-fields";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BankDAO() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void addAuthToken(HttpRequest.Builder builder) {
        String authHeader = com.chequeprint.util.Session.getAuthorizationHeader();
        if (authHeader != null && !authHeader.isBlank()) {
            builder.header("Authorization", authHeader);
        }
    }

    public List<Bank> findAll() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_BANKS))
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Bank>>() {});
        }
        return new ArrayList<>();
    }

    public Bank findById(int id) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_BANKS + "/" + id))
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), Bank.class);
        }
        return null;
    }

    public List<Map<String, Object>> findTemplatesByBankId(Long bankId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_TEMPLATES + "/bank/" + bankId))
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
        }
        return new ArrayList<>();
    }

    public List<Map<String, Object>> findTemplateFields(Long templateId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_TEMPLATE_FIELDS + "/" + templateId))
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
        }
        return new ArrayList<>();
    }

    public boolean saveTemplateFields(List<Map<String, Object>> fieldsPayload) throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(fieldsPayload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .uri(URI.create(API_TEMPLATE_FIELDS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public boolean update(Bank bank) throws Exception {
        if (bank == null || bank.getId() == null) return false;
        String jsonPayload = objectMapper.writeValueAsString(bank);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .uri(URI.create(API_BANKS + "/" + bank.getId()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addAuthToken(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
}