package com.chequeprint.service;

import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SinglePrintServiceTest {

    private PrintService printService;

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
        printService = new PrintService();
    }

    @Test
    public void testValidatePrinterFailsWhenNoPrinterSelected() {
        AppState.getInstance().setSelectedPrinter(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.validatePrinter();
        });

        assertTrue(ex.getMessage().contains("No printer selected"));
    }

    @Test
    public void testStep1LoadTemplateFailsWhenTemplateIsNull() {
        Cheque cheque = new Cheque(1, "Payee Name", new BigDecimal("1000.00"), 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.step1LoadTemplate(cheque, null, null);
        });

        assertTrue(ex.getMessage().contains("No cheque template layout loaded"));
    }

    @Test
    public void testStep2LoadChequeDataFailsWhenPayeeIsEmpty() {
        Cheque cheque = new Cheque(1, "  ", new BigDecimal("1000.00"), 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.step2LoadChequeData(cheque);
        });

        assertTrue(ex.getMessage().contains("Payee name is required"));
    }

    @Test
    public void testStep2LoadChequeDataSucceedsWithValidData() {
        Cheque cheque = new Cheque(1, "John Doe", new BigDecimal("5000.00"), 1, LocalDate.now());
        Cheque validated = printService.step2LoadChequeData(cheque);
        assertNotNull(validated);
        assertEquals("John Doe", validated.getPayeeName());
    }
}
