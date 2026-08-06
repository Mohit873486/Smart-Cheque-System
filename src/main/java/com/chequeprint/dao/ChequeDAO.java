package com.chequeprint.dao;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.HttpClientProvider;
import com.chequeprint.util.Session;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Cheque persistence operations.
 * Isolates low-level REST API HTTP communications for cheque CRUD tasks.
 */
public class ChequeDAO {

    private static final String API_CHEQUES = ApiConfig.BASE_URL + "/api/cheques";
    private static volatile long lastErrorLogTime = 0;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChequeDAO() {
        this.httpClient = HttpClientProvider.getClient(); // ✅ shared, no leak
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                if (value != null) {
                    gen.writeString(value.toString());
                } else {
                    gen.writeNull();
                }
            }
        });
        module.addDeserializer(LocalDate.class, new JsonDeserializer<>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String val = p.getValueAsString();
                return (val == null || val.isBlank()) ? null : LocalDate.parse(val);
            }
        });
        this.objectMapper.registerModule(module);
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
                if (i == maxRetries - 1)
                    throw e;
                Thread.sleep(500L * (i + 1)); // 500ms, 1000ms, 1500ms
            }
        }
        return null; // unreachable but compiler needs it
    }

    public List<Cheque> findAll() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_CHEQUES))
                    .header("Accept", "application/json");

            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), new TypeReference<List<Cheque>>() {
                });
            }
        } catch (Exception ex) {
            long now = System.currentTimeMillis();
            if (now - lastErrorLogTime > 5000) {
                lastErrorLogTime = now;
                System.err.println("ChequeDAO findAll error: " + ex.getMessage());
            }
        }
        return new ArrayList<>();
    }

    public Cheque findById(int id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_CHEQUES + "/" + id))
                    .header("Accept", "application/json");

            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), Cheque.class);
            }
        } catch (Exception ex) {
            System.err.println("ChequeDAO findById error: " + ex.getMessage());
        }
        return null;
    }

    public boolean insert(Cheque cheque) {
        try {
            String json = objectMapper.writeValueAsString(cheque);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_CHEQUES))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            System.err.println("ChequeDAO insert error: " + ex.getMessage());
            return false;
        }
    }

    public boolean update(Cheque cheque) {
        if (cheque == null || cheque.getId() <= 0)
            return false;
        try {
            String json = objectMapper.writeValueAsString(cheque);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_CHEQUES + "/" + cheque.getId()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            System.err.println("ChequeDAO update error: " + ex.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(API_CHEQUES + "/" + id));

            addAuthToken(builder);
            HttpResponse<String> response = sendWithRetry(builder.build());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            System.err.println("ChequeDAO delete error: " + ex.getMessage());
            return false;
        }
    }

    public boolean updateStatus(Cheque cheque, Cheque.Status status) {
        if (cheque == null)
            return false;
        cheque.setStatus(status);
        return update(cheque);
    }

    public boolean approveCheque(int id) {
        Cheque c = findById(id);
        if (c == null)
            return false;
        return updateStatus(c, Cheque.Status.Printed);
    }

    public boolean existsByChequeNo(String chequeNo, Integer excludeId) {
        if (chequeNo == null || chequeNo.isBlank())
            return false;
        List<Cheque> all = findAll();
        return all.stream().anyMatch(
                c -> chequeNo.equalsIgnoreCase(c.getChequeNo()) && (excludeId == null || !excludeId.equals(c.getId())));
    }

    public int countTotal() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_CHEQUES + "/stats/summary"))
                    .header("Accept", "application/json");
            addAuthToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                return node.get("total").asInt();
            }
        } catch (Exception ex) {
            System.err.println("ChequeDAO countTotal error: " + ex.getMessage());
        }
        return 0;
    }

    public int countPrinted() {
        return (int) findAll().stream().filter(c -> c.getStatus() == Cheque.Status.Printed).count();
    }

    public int countPending() {
        return (int) findAll().stream().filter(c -> c.getStatus() == Cheque.Status.Pending).count();
    }

    public int countTodayEntries() {
        LocalDate today = LocalDate.now();
        return (int) findAll().stream().filter(c -> today.equals(c.getIssueDate())).count();
    }

    public double sumThisMonth() {
        LocalDate now = LocalDate.now();
        return findAll().stream()
                .filter(c -> c.getIssueDate() != null && c.getIssueDate().getMonth() == now.getMonth()
                        && c.getIssueDate().getYear() == now.getYear())
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount().doubleValue() : 0.0)
                .sum();
    }

    public int countByIssueDate(LocalDate date) {
        if (date == null)
            return 0;
        return (int) findAll().stream().filter(c -> date.equals(c.getIssueDate())).count();
    }

    public List<Cheque> search(String query) {
        if (query == null || query.isBlank())
            return findAll();
        String needle = query.toLowerCase();
        return findAll().stream()
                .filter(c -> (c.getChequeNo() != null && c.getChequeNo().toLowerCase().contains(needle))
                        || (c.getPayeeName() != null && c.getPayeeName().toLowerCase().contains(needle)))
                .toList();
    }
}
