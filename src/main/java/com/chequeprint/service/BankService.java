package com.chequeprint.service;

import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.ApiClient;
import com.chequeprint.util.BankTemplateLayoutStore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service class for Bank Template operations, serving as the business logic layer
 * between Controllers and the REST API / BankTemplateLayoutStore.
 */
public class BankService {

    private final BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();
    private final ObjectMapper objectMapper;
    private static final String API_URL = ApiConfig.BASE_URL + "/api/banks";

    public BankService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Bank> getAll() throws Exception {
        String response = ApiClient.get(API_URL);
        return objectMapper.readValue(response,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Bank.class));
    }

    public Bank getById(int id) throws Exception {
        String response = ApiClient.get(API_URL + "/" + id);
        return objectMapper.readValue(response, Bank.class);
    }

    public void save(Bank bank, BankTemplateLayout layout, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        validate(bank);
        String code = bank.getBankCode().trim().toUpperCase();

        if (bank.getId() == null || bank.getId() == 0) {
            String json = objectMapper.writeValueAsString(bank);
            String response = ApiClient.post(API_URL, json);
            Bank created = objectMapper.readValue(response, Bank.class);
            bank.setId(created.getId());
        } else {
            // Find existing bank to handle code change
            Bank existing = getById(bank.getId());
            String oldCode = existing != null ? existing.getBankCode().trim().toUpperCase() : "";
            
            String json = objectMapper.writeValueAsString(bank);
            ApiClient.put(API_URL + "/" + bank.getId(), json);
            
            if (!oldCode.isEmpty() && !oldCode.equals(code)) {
                BankTemplateLayout moved = layoutByBankCode.remove(oldCode);
                if (moved != null) {
                    layoutByBankCode.put(code, moved);
                }
            }
        }

        layoutByBankCode.put(code, layout.copy());
        layoutStore.saveAll(layoutByBankCode);
    }

    public void delete(Bank bank, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        ApiClient.delete(API_URL + "/" + bank.getId());
        String code = bank.getBankCode() != null ? bank.getBankCode().trim().toUpperCase() : "";
        if (!code.isEmpty()) {
            layoutByBankCode.remove(code);
        }
        layoutStore.saveAll(layoutByBankCode);
    }

    public Map<String, BankTemplateLayout> loadAllLayouts() {
        return layoutStore.loadAll();
    }

    public void saveLayouts(Map<String, BankTemplateLayout> layouts) throws IOException {
        layoutStore.saveAll(layouts);
    }

    private void validate(Bank bank) {
        if (bank.getBankName() == null || bank.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        if (bank.getBankCode() == null || bank.getBankCode().isBlank()) {
            throw new IllegalArgumentException("Bank code is required.");
        }
    }
}
