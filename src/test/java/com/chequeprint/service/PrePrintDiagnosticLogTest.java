package com.chequeprint.service;

import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PrePrintDiagnosticLogTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testLogPrePrintDiagnosticsOutputsRequiredFields() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            Cheque cheque = new Cheque(1, "Acme Corp", new BigDecimal("12500.00"), 1, LocalDate.now());
            BankTemplateLayout layout = new BankTemplateLayout();

            ChequePrintPipeline.logPrePrintDiagnostics(cheque, layout);

            String logOutput = outContent.toString();

            assertTrue(logOutput.contains("[PRE-PRINT DIAGNOSTICS LOG]"));
            assertTrue(logOutput.contains("Selected Printer Name"));
            assertTrue(logOutput.contains("Template Loaded"));
            assertTrue(logOutput.contains("Cheque Data Status"));
            assertTrue(logOutput.contains("Coordinates Applied"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
