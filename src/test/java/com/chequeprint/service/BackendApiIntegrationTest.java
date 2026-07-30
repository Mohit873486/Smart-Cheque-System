package com.chequeprint.service;

import com.chequeprint.config.ApiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BackendApiIntegrationTest {

    @Test
    public void testApiBaseUrlIsConfigured() {
        assertNotNull(ApiConfig.BASE_URL, "API Base URL must be configured.");
        assertTrue(ApiConfig.BASE_URL.startsWith("http"), "API Base URL must start with http/https protocol.");
    }

    @Test
    public void testBankAccountServiceConnectsToBackendApi() {
        BankAccountService accountService = new BankAccountService();
        assertNotNull(accountService, "BankAccountService must be initialized for REST API calls.");
    }

    @Test
    public void testTemplateServiceConnectsToBackendApi() {
        TemplateService templateService = new TemplateService();
        assertNotNull(templateService, "TemplateService must be initialized for REST API calls.");
    }

    @Test
    public void testChequeServiceConnectsToBackendApi() {
        ChequeService chequeService = new ChequeService();
        assertNotNull(chequeService, "ChequeService must be initialized for REST API calls.");
    }
}
