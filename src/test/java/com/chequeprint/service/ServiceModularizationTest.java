package com.chequeprint.service;

import com.chequeprint.ai.AIService;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceModularizationTest {

    private TemplateService templateService;
    private PrintService printService;
    private ChequeService chequeService;
    private UserService userService;
    private AIService aiService;

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
        templateService = new TemplateService();
        printService = new PrintService();
        chequeService = new ChequeService();
        userService = new UserService();
        aiService = new AIService();
    }

    @Test
    public void testTemplateServiceProvidesDefaultLayout() {
        BankTemplateLayout layout = templateService.getDefaultLayout();
        assertNotNull(layout, "Default template layout must not be null.");
        assertEquals(8.0, layout.getWidthInches());
    }

    @Test
    public void testPrintServiceHandlesPrinterValidation() {
        AppState.getInstance().setSelectedPrinter(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.validatePrinter();
        });
        assertTrue(ex.getMessage().contains("No printer selected"));
    }

    @Test
    public void testAiServiceRelocatedToAiPackage() {
        assertNotNull(aiService, "com.chequeprint.ai.AIService instance must not be null.");
    }
}
