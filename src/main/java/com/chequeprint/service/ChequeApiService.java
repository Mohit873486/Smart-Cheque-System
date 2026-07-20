package com.chequeprint.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to interact with the Cheque REST API.
 */
public class ChequeApiService {

    private static final String API_BASE_URL = "http://localhost:8080/api/cheque";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChequeApiService() {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a new cheque via POST /api/cheque
     */
    public ChequeApiResponse createCheque(String payeeName, double amount, String dateStr) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("payeeName", payeeName);
            payload.put("amount", amount);
            
            // Map natural language dates
            String actualDate = mapNaturalDate(dateStr);
            payload.put("issueDate", actualDate);
            payload.put("status", "PENDING"); // Default status

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return ChequeApiResponse.success("Cheque for " + payeeName + " created successfully!");
            } else {
                return ChequeApiResponse.error("Failed to create cheque. Server responded with: " + response.statusCode());
            }
        } catch (Exception e) {
            return ChequeApiResponse.error("Error connecting to server: " + e.getMessage());
        }
    }

    /**
     * Get all cheques via GET /api/cheque (but not testing )
     */
    public ChequeApiResponse getAllCheques() {
        return fetchCheques(false);
    }

    /**
     * Get pending cheques via GET /api/cheque
     */
    public ChequeApiResponse getPendingCheques() {
        return fetchCheques(true);
    }

    private ChequeApiResponse fetchCheques(boolean onlyPending) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> cheques = objectMapper.readValue(response.body(), new TypeReference<>() {});
                
                if (cheques.isEmpty()) {
                    return ChequeApiResponse.success("No cheques found in the database.");
                }

                // Filter pending if requested
                List<Map<String, Object>> filtered = cheques;
                if (onlyPending) {
                    filtered = cheques.stream()
                        .filter(c -> "PENDING".equalsIgnoreCase(String.valueOf(c.get("status"))))
                        .toList();
                }
                
                if (filtered.isEmpty()) {
                    return ChequeApiResponse.success("No " + (onlyPending ? "pending " : "") + "cheques found.");
                }
                
                return ChequeApiResponse.success("Found " + filtered.size() + " cheques.", filtered);
            } else {
                return ChequeApiResponse.error("Failed to fetch cheques. Server status: " + response.statusCode());
            }
        } catch (Exception e) {
            return ChequeApiResponse.error("Error fetching cheques: " + e.getMessage());
        }
    }

    /**
     * Delete a cheque via DELETE /api/cheque/{id}
     */
    public ChequeApiResponse deleteCheque(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/" + id))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                return ChequeApiResponse.success("Cheque ID " + id + " deleted successfully.");
            } else if (response.statusCode() == 404) {
                return ChequeApiResponse.error("Cheque ID " + id + " not found.");
            } else {
                return ChequeApiResponse.error("Failed to delete cheque. Server status: " + response.statusCode());
            }
        } catch (Exception e) {
            return ChequeApiResponse.error("Error deleting cheque: " + e.getMessage());
        }
    }

    // Helper to map "tomorrow" -> actual date string for backend
    private String mapNaturalDate(String dateStr) {
        if (dateStr == null) return LocalDate.now().toString();
        String lower = dateStr.toLowerCase().trim();
        if (lower.equals("tomorrow")) {
            return LocalDate.now().plusDays(1).toString();
        } else if (lower.equals("today")) {
            return LocalDate.now().toString();
        } else if (lower.equals("yesterday")) {
            return LocalDate.now().minusDays(1).toString();
        }
        // Assuming it's already a valid date or let the backend fail
        return dateStr;
    }
}
