package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ProfessionalPrintFlowTest {

    private PrintService printService;

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
        printService = new PrintService();
    }

    @Test
    public void testProfessionalPrintFlowFailsAtStage1IfNoPrinterSelected() {
        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Test Payee");
        cheque.setAmount(new BigDecimal("5000.00"));
        cheque.setIssueDate(LocalDate.now());

        AppState.getInstance().setSelectedPrinter(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            printService.executeProfessionalPrintFlow(cheque, bank, new BankTemplateLayout(), null);
        });

        assertTrue(ex.getMessage().contains("No printer selected"), "Stage 1 must validate printer selection.");
    }
}
