package com.chequeprint.service;

import com.chequeprint.model.Bank;
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

public class ChequePrintPipelineTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testStep1FailsWhenNoTemplateLoaded() {
        Cheque cheque = new Cheque(1, "Payee Name", new BigDecimal("1000.00"), 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ChequePrintPipeline.step1LoadTemplate(cheque, null, null);
        });

        assertTrue(ex.getMessage().contains("Step 1"));
    }

    @Test
    public void testStep1SucceedsWhenTemplateLoaded() {
        Cheque cheque = new Cheque(1, "Payee Name", new BigDecimal("1000.00"), 1, LocalDate.now());
        BankTemplateLayout layout = new BankTemplateLayout();

        BankTemplateLayout loaded = ChequePrintPipeline.step1LoadTemplate(cheque, null, layout);
        assertNotNull(loaded);
        assertEquals(8.0, loaded.getWidthInches());
    }

    @Test
    public void testStep2FailsWhenPayeeIsEmpty() {
        Cheque cheque = new Cheque(1, "  ", new BigDecimal("1000.00"), 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ChequePrintPipeline.step2LoadChequeData(cheque);
        });

        assertTrue(ex.getMessage().contains("Step 2"));
    }

    @Test
    public void testStep2FailsWhenAmountIsZeroOrNegative() {
        Cheque cheque = new Cheque(1, "Payee Name", BigDecimal.ZERO, 1, LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ChequePrintPipeline.step2LoadChequeData(cheque);
        });

        assertTrue(ex.getMessage().contains("Step 2"));
    }

    @Test
    public void testStep3MergesChequeDataIntoTemplate() {
        Cheque cheque = new Cheque(1, "John Doe", new BigDecimal("5000.00"), 1, LocalDate.now());
        BankTemplateLayout layout = new BankTemplateLayout();

        BankTemplateLayout merged = ChequePrintPipeline.step3MergeDataIntoTemplate(cheque, null, layout);
        assertNotNull(merged);
    }

    @Test
    public void testStep5FailsWhenNoPrinterSelected() {
        AppState.getInstance().setSelectedPrinter(null);
        javafx.scene.layout.Pane dummyCanvas = new javafx.scene.layout.Pane();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ChequePrintPipeline.step5SendToSelectedPrinter(dummyCanvas, null);
        });

        assertTrue(ex.getMessage().contains("Step 5"));
    }
}
