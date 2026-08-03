package com.chequeprint.service;

import com.chequeprint.api.ApiGateway;
import com.chequeprint.api.ApiResponse;
import com.chequeprint.config.ApiConfig;
import com.chequeprint.model.BankAccount;
import com.chequeprint.model.ChequeTemplate;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApiService {

    private static final String BANK_ACCOUNT_URL = ApiConfig.BASE_URL + "/api/bank/account";
    private static final String TEMPLATE_URL = ApiConfig.BASE_URL + "/api/template";

    private final ApiGateway apiGateway;

    public ApiService() {
        this(new ApiGateway());
    }

    public ApiService(ApiGateway apiGateway) {
        this.apiGateway = apiGateway;
    }

    public List<BankAccount> getBankAccounts() throws Exception {
        try {
            ApiResponse<List<BankAccount>> response = apiGateway.get(
                    BANK_ACCOUNT_URL,
                    new TypeReference<List<BankAccount>>() {});

            if (response.getStatusCode() == 404 || response.getStatusCode() == 409) {
                return Collections.emptyList();
            }
            response.throwIfFailed("Load bank accounts");
            return response.getBody().orElseGet(Collections::emptyList);
        } catch (SocketTimeoutException ex) {
            throw new SocketTimeoutException("Bank account API timed out: " + BANK_ACCOUNT_URL);
        }
    }

    public BankAccount saveBankAccount(BankAccount account) throws Exception {
        ApiResponse<BankAccount> response = apiGateway.post(BANK_ACCOUNT_URL, account, BankAccount.class);
        response.throwIfFailed("Save bank account");
        return response.requireBody("Save bank account returned an empty response.");
    }

    public BankAccount updateBankAccount(Integer id, BankAccount account) throws Exception {
        ApiResponse<BankAccount> response = apiGateway.put(BANK_ACCOUNT_URL + "/" + id, account, BankAccount.class);
        response.throwIfFailed("Update bank account");
        return response.requireBody("Update bank account returned an empty response.");
    }

    public void deleteBankAccount(Integer id) throws Exception {
        ApiResponse<Void> response = apiGateway.delete(BANK_ACCOUNT_URL + "/" + id);
        response.throwIfFailed("Delete bank account");
    }

    public Optional<ChequeTemplate> findChequeTemplateByBankId(Long bankId) throws Exception {
        if (bankId == null || bankId <= 0) {
            return Optional.empty();
        }
        ApiResponse<ChequeTemplate> response = apiGateway.get(TEMPLATE_URL + "/bank/" + bankId, ChequeTemplate.class);
        if (response.getStatusCode() == 404) {
            return Optional.empty();
        }
        response.throwIfFailed("Load cheque template");
        return response.getBody();
    }

    public ChequeTemplate saveChequeTemplate(ChequeTemplate template) throws Exception {
        ApiResponse<ChequeTemplate> response = apiGateway.post(TEMPLATE_URL + "/save", template, ChequeTemplate.class);
        response.throwIfFailed("Save cheque template");
        return response.requireBody("Save cheque template returned an empty response.");
    }

    public List<ChequeTemplate> getTemplatesByAccountId(Long accountId) throws Exception {
        if (accountId == null || accountId <= 0) {
            return Collections.emptyList();
        }
        ApiResponse<List<ChequeTemplate>> response = apiGateway.get(
                TEMPLATE_URL + "/account/" + accountId,
                new TypeReference<List<ChequeTemplate>>() {});

        if (response.getStatusCode() == 404) {
            return Collections.emptyList();
        }
        response.throwIfFailed("Load account templates");
        return response.getBody().orElseGet(Collections::emptyList);
    }

    public void setDefaultTemplate(Long accountId, Long templateId) throws Exception {
        ApiResponse<Void> response = apiGateway.putNoBody(
                TEMPLATE_URL + "/account/" + accountId + "/default/" + templateId);
        response.throwIfFailed("Set default template");
    }
}
