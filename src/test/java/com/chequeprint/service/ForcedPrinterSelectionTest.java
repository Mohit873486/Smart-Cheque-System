package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ForcedPrinterSelectionTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testAppStateDoesNotAutoSelectOSDefaultPrinterWhenCleared() {
        AppState.getInstance().clear();
        assertNull(AppState.getInstance().getSelectedPrinter(), "AppState selectedPrinter must be null when no printer is explicitly chosen by user.");
    }

    @Test
    public void testPrintPipelineBlocksWhenAppStatePrinterIsNull() {
        AppState.getInstance().setSelectedPrinter(null);

        Cheque cheque = new Cheque(1, "Payee Name", new BigDecimal("1000.00"), 1, LocalDate.now());
        BankTemplateLayout layout = new BankTemplateLayout();
        layout.setBankCode("SBI");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ChequePrintPipeline.execute(cheque, new Bank(), layout, null);
        });

        assertTrue(ex.getMessage().contains("No printer selected"));
    }
}
