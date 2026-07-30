package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.AppState;
import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewChequeWindowTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testNewWindowInheritsAppStateDataWithoutReinitializingEmptyValues() {
        Bank activeBank = new Bank(1, "HDFC Bank", "HDFC", 8.0, 3.66, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        BankTemplateLayout activeLayout = new BankTemplateLayout();
        activeLayout.setBankCode("HDFC");

        AppState.getInstance().setSelectedBank(activeBank);
        AppState.getInstance().setSelectedTemplate(activeLayout);

        // When a new window opens, reading from AppState must return stored active bank and template
        assertEquals("HDFC Bank", AppState.getInstance().getSelectedBank().getBankName());
        assertNotNull(AppState.getInstance().getSelectedTemplate());
        assertEquals("HDFC", AppState.getInstance().getSelectedTemplate().getBankCode());
    }

    @Test
    public void testPrintActionBlockedWhenPrinterIsMissing() {
        AppState.getInstance().setSelectedPrinter(null);
        Bank activeBank = new Bank(1, "HDFC Bank", "HDFC", 8.0, 3.66, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        BankTemplateLayout activeLayout = new BankTemplateLayout();
        AppState.getInstance().setSelectedBank(activeBank);
        AppState.getInstance().setSelectedTemplate(activeLayout);

        com.chequeprint.service.PrintService printService = new com.chequeprint.service.PrintService();
        com.chequeprint.model.Cheque cheque = new com.chequeprint.model.Cheque(1, "Test Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.validateBeforePrint(cheque, activeLayout);
        });

        assertTrue(ex.getMessage().contains("No printer selected"));
    }
}
