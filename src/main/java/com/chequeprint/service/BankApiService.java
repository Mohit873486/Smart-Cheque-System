package com.chequeprint.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chequeprint.model.BankAccount; 

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Service class for calling the Bank REST API.
 * Uses Java 11+ HttpClient and Jackson for JSON parsing.
 */
public class BankApiService {

    private static final String BASE_URL = "http://localhost:8080/api/bank/account";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BankApiService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<BankAccount> getAllBanks() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), new TypeReference<List<BankAccount>>() {});
        } else {
            throw new RuntimeException("Failed to fetch bank accounts. Status: " + response.statusCode());
        }
    }

    public BankAccount createBank(BankAccount bank) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(bank);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), BankAccount.class);
        } else {
            throw new RuntimeException("Failed to create bank. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    public BankAccount updateBank(Integer id, BankAccount bank) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(bank);

        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), BankAccount.class);
        } else {
            throw new RuntimeException("Failed to update bank. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    public void deleteBank(Integer id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE_URL + "/" + id))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("Failed to delete bank. Status: " + response.statusCode());
        }
    }
}
