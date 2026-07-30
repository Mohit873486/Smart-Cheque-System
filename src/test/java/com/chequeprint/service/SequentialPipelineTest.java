package com.chequeprint.service;

import com.chequeprint.engine.ChequeRenderEngine;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.state.AppState;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SequentialPipelineTest {

    private TemplateService templateService;
    private ChequeService chequeService;
    private PrintService printService;

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
        templateService = new TemplateService();
        chequeService = new ChequeService();
        printService = new PrintService();
    }

    @Test
    public void testSequentialPipelineExecution() {
        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Test Recipient");
        cheque.setAmount(new BigDecimal("25000.00"));
        cheque.setIssueDate(LocalDate.now());

        // Step 1: TemplateService loads template
        BankTemplateLayout layout = templateService.getTemplateForBank(bank);
        assertNotNull(layout, "Step 1: TemplateService must load template.");

        // Step 2: ChequeService validates cheque data
        Cheque validatedCheque = chequeService.validateChequeData(cheque);
        assertNotNull(validatedCheque, "Step 2: ChequeService must validate cheque data.");

        // Step 3: ChequeRenderEngine renders canvas
        Pane canvas = new Pane();
        ChequeRenderEngine.renderCheque(canvas, validatedCheque, bank, layout);
        assertFalse(canvas.getChildren().isEmpty(), "Step 3: ChequeRenderEngine must render cheque layout.");

        // Step 4: PrintService fails gracefully if no printer is selected
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.printRenderedCheque(canvas, null);
        });
        assertTrue(ex.getMessage().contains("No printer selected"));
    }
}
