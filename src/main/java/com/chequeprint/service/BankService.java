package com.chequeprint.service;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.BankTemplateLayoutStore;
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
import java.util.stream.Collectors;

/**
 * Service class for Bank REST API operations and template management in JavaFX.
 * Uses Java HttpClient to make REST API calls to the backend server.
 */
public class BankService {

    private static final String API_URL = ApiConfig.BASE_URL + "/api/banks";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BankTemplateLayoutStore layoutStore;

    public BankService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.layoutStore = new BankTemplateLayoutStore();
    }

    /**
     * GET bank list - Fetches all banks from REST API.
     * @return List of Bank objects
     */
    public List<Bank> getBanks() throws Exception {
        return getAll();
    }

    public static List<Bank> getBanksStatic() throws Exception {
        return new BankService().getAll();
    }

    public List<Bank> getAllBanks() throws Exception {
        return getAll();
    }

    /**
     * GET bank list - Fetches all banks from REST API.
     * @return List of Bank objects
     */
    public List<Bank> getAll() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json");

        addAuthToken(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Bank>>() {});
        } else if (response.statusCode() == 404 || response.statusCode() == 409) {
            return new ArrayList<>();
        } else {
            throw new RuntimeException("Failed to fetch bank list. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    /**
     * GET bank by ID from REST API.
     */
    public Bank getById(int id) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_URL + "/" + id))
                .header("Accept", "application/json");

        addAuthToken(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), Bank.class);
        } else if (response.statusCode() == 404) {
            return null;
        } else {
            throw new RuntimeException("Failed to get bank with id " + id + ". Status: " + response.statusCode());
        }
    }

    /**
     * POST new bank - Creates a new bank via REST API.
     * @param bank Bank object to create
     * @return Created Bank object
     */
    public Bank createBank(Bank bank) throws Exception {
        validate(bank);
        String jsonBody = objectMapper.writeValueAsString(bank);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addAuthToken(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Bank.class);
        } else {
            throw new RuntimeException("Failed to create bank. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    /**
     * PUT update bank - Updates an existing bank via REST API.
     * @param id Bank ID
     * @param bank Updated bank data
     * @return Updated Bank object
     */
    public Bank updateBank(int id, Bank bank) throws Exception {
        validate(bank);
        bank.setId(id);
        String jsonBody = objectMapper.writeValueAsString(bank);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .uri(URI.create(API_URL + "/" + id))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addAuthToken(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), Bank.class);
        } else {
            throw new RuntimeException("Failed to update bank. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    /**
     * DELETE bank - Deletes a bank by ID via REST API.
     * @param id Bank ID to delete
     */
    public void deleteBank(int id) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(API_URL + "/" + id));

        addAuthToken(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("Failed to delete bank. Status: " + response.statusCode());
        }
    }

    /**
     * Returns list of all bank names for drop-down combo boxes.
     */
    public List<String> findAllNames() throws Exception {
        List<Bank> banks = getAllBanks();
        return banks.stream()
                .map(Bank::getBankName)
                .collect(Collectors.toList());
    }

    /**
     * Saves bank and updates layout store mappings.
     */
    public void save(Bank bank, BankTemplateLayout layout, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        validate(bank);
        String code = bank.getBankCode().trim().toUpperCase();

        if (bank.getId() == null || bank.getId() == 0) {
            Bank created = createBank(bank);
            bank.setId(created.getId());
        } else {
            Bank existing = getById(bank.getId());
            String oldCode = existing != null ? existing.getBankCode().trim().toUpperCase() : "";

            updateBank(bank.getId(), bank);

            if (!oldCode.isEmpty() && !oldCode.equals(code)) {
                BankTemplateLayout moved = layoutByBankCode.remove(oldCode);
                if (moved != null) {
                    layoutByBankCode.put(code, moved);
                }
            }
        }

        if (layout != null && layoutByBankCode != null) {
            layoutByBankCode.put(code, layout.copy());
            layoutStore.saveAll(layoutByBankCode);
        }
    }

    /**
     * Deletes bank and updates layout store mappings.
     */
    public void delete(Bank bank, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        if (bank != null && bank.getId() != null) {
            deleteBank(bank.getId());
            String code = bank.getBankCode() != null ? bank.getBankCode().trim().toUpperCase() : "";
            if (!code.isEmpty() && layoutByBankCode != null) {
                layoutByBankCode.remove(code);
                layoutStore.saveAll(layoutByBankCode);
            }
        }
    }

    public Map<String, BankTemplateLayout> loadAllLayouts() {
        return layoutStore.loadAll();
    }

    public void saveLayouts(Map<String, BankTemplateLayout> layouts) throws Exception {
        layoutStore.saveAll(layouts);
    }

    private void addAuthToken(HttpRequest.Builder builder) {
        String authHeader = com.chequeprint.util.Session.getAuthorizationHeader();
        if (!authHeader.isBlank()) {
            builder.header("Authorization", authHeader);
        }
    }

    private void validate(Bank bank) {
        if (bank == null) {
            throw new IllegalArgumentException("Bank object cannot be null.");
        }
        if (bank.getBankName() == null || bank.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        if (bank.getBankCode() == null || bank.getBankCode().isBlank()) {
            throw new IllegalArgumentException("Bank code is required.");
        }
    }
}
