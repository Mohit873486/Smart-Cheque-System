package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class InteractivePrintWorkflowTest {

    private PrintService printService;

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
        printService = new PrintService();
    }

    @Test
    public void testInteractivePrintWorkflowFailsStep1ForInvalidCheque() {
        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque invalidCheque = new Cheque(); // Null payee, zero amount

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.executeInteractivePrintWorkflow(invalidCheque, bank, new BankTemplateLayout(), null);
        });

        assertTrue(ex.getMessage().contains("Payee"), "Step 1 must validate cheque data before generating preview.");
    }
}
