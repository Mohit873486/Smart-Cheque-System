package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import javafx.application.Platform;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JasperPrintUtilTest {

    @BeforeAll
    public static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Test
    public void testPrintChequeWithPrinterOutcome() throws Exception {
        Cheque cheque = new Cheque(1, "Payee", new BigDecimal("100.00"), 1, LocalDate.now());
        cheque.setStatus(Cheque.Status.Approved);
        Bank bank = new Bank(1, "Test Bank", "Template", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        Printer mockPrinter = mock(Printer.class);
        when(mockPrinter.getName()).thenReturn("TestMockPrinter");

        PrinterJob mockJob = mock(PrinterJob.class);

        JasperPrint dummyPrint = new JasperPrint();
        dummyPrint.setPageWidth(100);
        dummyPrint.setPageHeight(50);

        try (MockedStatic<PrinterJob> printerJobMock = mockStatic(PrinterJob.class);
             MockedStatic<JasperPrintUtil> jasperPrintUtilMock = mockStatic(JasperPrintUtil.class, CALLS_REAL_METHODS);
             MockedStatic<JasperPrintManager> jasperPrintManagerMock = mockStatic(JasperPrintManager.class)) {

            printerJobMock.when(PrinterJob::createPrinterJob).thenReturn(mockJob);
            jasperPrintUtilMock.when(() -> JasperPrintUtil.generateChequePrintObject(any(), any())).thenReturn(dummyPrint);

            BufferedImage dummyImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            jasperPrintManagerMock.when(() -> JasperPrintManager.printPageToImage(any(), anyInt(), anyFloat())).thenReturn(dummyImg);

            // Test case 1: printPage returns true, endJob returns true
            when(mockJob.printPage(any())).thenReturn(true);
            when(mockJob.endJob()).thenReturn(true);

            boolean resultSuccess = JasperPrintUtil.printCheque(cheque, bank, mockPrinter);
            assertTrue(resultSuccess, "printCheque should return true when PrinterJob finishes successfully.");
            assertEquals(Cheque.Status.Printed, cheque.getStatus(), "Cheque status should be set to Printed upon success.");

            // Test case 2: printPage returns false, endJob returns false
            cheque.setStatus(Cheque.Status.Approved);
            when(mockJob.printPage(any())).thenReturn(false);
            when(mockJob.endJob()).thenReturn(false);

            boolean resultFailure = JasperPrintUtil.printCheque(cheque, bank, mockPrinter);
            assertFalse(resultFailure, "printCheque should return false when PrinterJob fails.");
            assertEquals(Cheque.Status.Approved, cheque.getStatus(), "Cheque status should not change on print failure.");
        }
    }
}
