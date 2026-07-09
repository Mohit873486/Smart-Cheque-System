package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.JasperPrintUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PrintServiceTest {

    private ChequeDAO chequeDAO;
    private BankDAO bankDAO;
    private PrintService printService;
    private MockedStatic<JasperPrintUtil> jasperPrintUtilMock;

    @BeforeEach
    public void setUp() {
        chequeDAO = mock(ChequeDAO.class);
        bankDAO = mock(BankDAO.class);
        printService = new PrintService(chequeDAO, bankDAO);
        jasperPrintUtilMock = mockStatic(JasperPrintUtil.class);
    }

    @AfterEach
    public void tearDown() {
        jasperPrintUtilMock.close();
    }

    @Test
    public void testPreviewChequeStatusLifecycle() throws Exception {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Pending);
        Bank bank = new Bank(1, "Test Bank", "Template", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        when(bankDAO.findById(1)).thenReturn(bank);

        // Case 1: Print outcome is true -> status is updated to Printed
        jasperPrintUtilMock.when(() -> JasperPrintUtil.previewCheque(any(), any())).thenReturn(true);
        boolean resultTrue = printService.previewCheque(cheque);
        assertTrue(resultTrue);
        verify(chequeDAO, times(1)).updateStatus(cheque, Cheque.Status.Printed);

        // Reset verify count
        Mockito.clearInvocations(chequeDAO);

        // Case 2: Print outcome is false -> status is NOT updated to Printed
        jasperPrintUtilMock.when(() -> JasperPrintUtil.previewCheque(any(), any())).thenReturn(false);
        boolean resultFalse = printService.previewCheque(cheque);
        assertFalse(resultFalse);
        verify(chequeDAO, never()).updateStatus(any(), any());
    }

    @Test
    public void testPrintChequeUpdatesStatusOnlyOnSuccess() throws Exception {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Approved);
        Bank bank = new Bank(1, "Test Bank", "Template", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        when(bankDAO.findById(1)).thenReturn(bank);

        // Success case
        jasperPrintUtilMock.when(() -> JasperPrintUtil.printCheque(any(), any())).thenReturn(true);
        boolean success = printService.printCheque(cheque);
        assertTrue(success);
        verify(chequeDAO, times(1)).updateStatus(cheque, Cheque.Status.Printed);

        // Reset mock/verify history
        Mockito.clearInvocations(chequeDAO);

        // Failure case
        jasperPrintUtilMock.when(() -> JasperPrintUtil.printCheque(any(), any())).thenReturn(false);
        boolean failure = printService.printCheque(cheque);
        assertFalse(failure);
        verify(chequeDAO, never()).updateStatus(any(), any());
    }

    @Test
    public void testPrintAllPendingThrowsExceptionOnFailure() throws Exception {
        Cheque cheque1 = new Cheque(1, "Payee One", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque1.setChequeNo("CHQ001");
        cheque1.setStatus(Cheque.Status.Pending);

        Cheque cheque2 = new Cheque(2, "Payee Two", new java.math.BigDecimal("200.00"), 1, java.time.LocalDate.now());
        cheque2.setChequeNo("CHQ002");
        cheque2.setStatus(Cheque.Status.Pending);

        Bank bank = new Bank(1, "Test Bank", "Template", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        when(chequeDAO.findAll()).thenReturn(Arrays.asList(cheque1, cheque2));
        when(bankDAO.findById(1)).thenReturn(bank);

        // Simulate success for cheque1 and exception for cheque2
        jasperPrintUtilMock.when(() -> JasperPrintUtil.printCheque(eq(cheque1), any())).thenReturn(true);
        jasperPrintUtilMock.when(() -> JasperPrintUtil.printCheque(eq(cheque2), any())).thenThrow(new RuntimeException("Printer error"));

        BatchPrintException ex = assertThrows(BatchPrintException.class, () -> {
            printService.printAllPending();
        });

        assertEquals(1, ex.getSuccesses().size());
        assertEquals(cheque1, ex.getSuccesses().get(0));
        assertEquals(1, ex.getFailures().size());
        assertTrue(ex.getFailures().get(0).contains("CHQ002"));
        assertTrue(ex.getFailures().get(0).contains("Printer error"));

        verify(chequeDAO, times(1)).updateStatus(cheque1, Cheque.Status.Printed);
        verify(chequeDAO, never()).updateStatus(cheque2, Cheque.Status.Printed);
    }
}
