package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PrintServicePreconditionTest {

    private PrintService printService;

    @BeforeEach
    public void setUp() {
        ChequeDAO chequeDAO = mock(ChequeDAO.class);
        BankDAO bankDAO = mock(BankDAO.class);
        printService = new PrintService(chequeDAO, bankDAO);
    }

    @Test
    public void testValidateFailsWhenNoPrinterSelected() {
        AppState.getInstance().setSelectedPrinter(null);

        BankTemplateLayout layout = new BankTemplateLayout();
        Cheque cheque = new Cheque(1, "John Doe", new BigDecimal("500.00"), 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.validateBeforePrint(cheque, layout);
        });

        assertTrue(ex.getMessage().contains("No printer selected"));
    }

    @Test
    public void testValidateFailsWhenChequePayeeIsEmpty() {
        // Set a valid printer if available on system
        Printer validPrinter = PrinterUtils.getDefaultValidPrinter();
        if (validPrinter != null) {
            AppState.getInstance().setSelectedPrinter(validPrinter);

            BankTemplateLayout layout = new BankTemplateLayout();
            Cheque invalidCheque = new Cheque(1, "   ", new BigDecimal("500.00"), 1, LocalDate.now());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                printService.validateBeforePrint(invalidCheque, layout);
            });

            assertTrue(ex.getMessage().contains("Payee name is required"));
        }
    }

    @Test
    public void testValidateFailsWhenChequeAmountZeroOrNegative() {
        Printer validPrinter = PrinterUtils.getDefaultValidPrinter();
        if (validPrinter != null) {
            AppState.getInstance().setSelectedPrinter(validPrinter);

            BankTemplateLayout layout = new BankTemplateLayout();
            Cheque zeroCheque = new Cheque(1, "John Doe", BigDecimal.ZERO, 1, LocalDate.now());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                printService.validateBeforePrint(zeroCheque, layout);
            });

            assertTrue(ex.getMessage().contains("Amount must be greater than zero"));
        }
    }

    @Test
    public void testValidateFailsWhenNoTemplateLoaded() {
        Printer validPrinter = PrinterUtils.getDefaultValidPrinter();
        if (validPrinter != null) {
            AppState.getInstance().setSelectedPrinter(validPrinter);
            AppState.getInstance().setSelectedTemplate(null);

            Cheque validCheque = new Cheque(1, "John Doe", new BigDecimal("500.00"), 1, LocalDate.now());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                printService.validateBeforePrint(validCheque, null);
            });

            assertTrue(ex.getMessage().contains("No cheque template layout loaded"));
        }
    }
}
