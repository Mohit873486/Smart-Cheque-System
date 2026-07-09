package com.chequeprint.service;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.util.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChequeWorkflowServiceTest {

    private ChequeService chequeService;
    private PrintService printService;
    private AuditService auditService;
    private ChequeWorkflowService workflowService;

    private User adminActor;
    private User managerActor;
    private User regularActor;
    private User auditorActor;

    @BeforeEach
    public void setUp() {
        chequeService = mock(ChequeService.class);
        printService = mock(PrintService.class);
        auditService = mock(AuditService.class);
        workflowService = new ChequeWorkflowService(chequeService, printService, auditService);

        adminActor = new User();
        adminActor.setRole("Admin");

        managerActor = new User();
        managerActor.setRole("Manager");

        regularActor = new User();
        regularActor.setRole("User");

        auditorActor = new User();
        auditorActor.setRole("Auditor");
    }

    @Test
    public void testCreatePendingSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Draft);

        when(chequeService.save(any(Cheque.class))).thenReturn(true);

        Cheque result = workflowService.createPending(cheque, regularActor);

        assertEquals(Cheque.Status.Pending, result.getStatus());
        verify(chequeService, times(1)).save(cheque);
    }

    @Test
    public void testCreatePendingPermissionDenied() {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        
        assertThrows(AccessDeniedException.class, () -> {
            workflowService.createPending(cheque, auditorActor);
        });
    }

    @Test
    public void testApproveSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Pending);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.approveCheque(1)).thenReturn(true);

        workflowService.approve(1, managerActor);

        verify(chequeService, times(1)).approveCheque(1);
    }

    @Test
    public void testApproveIllegalState() {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Approved); // Not pending

        when(chequeService.getById(1)).thenReturn(cheque);

        assertThrows(IllegalStateException.class, () -> {
            workflowService.approve(1, managerActor);
        });
    }

    @Test
    public void testApprovePermissionDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            workflowService.approve(1, regularActor);
        });
    }

    @Test
    public void testRejectSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Pending);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.setStatus(cheque, Cheque.Status.Rejected)).thenReturn(true);

        workflowService.reject(1, managerActor);

        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Rejected);
    }

    @Test
    public void testRejectPermissionDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            workflowService.reject(1, regularActor);
        });
    }

    @Test
    public void testPrintSuccess() throws Exception {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Approved); // Must be approved first

        when(chequeService.getById(1)).thenReturn(cheque);
        when(printService.previewCheque(cheque)).thenReturn(true);
        when(chequeService.setStatus(cheque, Cheque.Status.Printed)).thenReturn(true);

        workflowService.print(1, managerActor);

        verify(printService, times(1)).previewCheque(cheque);
        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Printed);
    }

    @Test
    public void testPrintSeparationOfDutiesIllegalState() {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Pending); // Must be Approved/Printed, so Pending throws exception

        when(chequeService.getById(1)).thenReturn(cheque);

        assertThrows(IllegalStateException.class, () -> {
            workflowService.print(1, managerActor);
        });
    }

    @Test
    public void testPrintPermissionDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            workflowService.print(1, regularActor);
        });
    }

    @Test
    public void testDepositSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Printed);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.setStatus(cheque, Cheque.Status.Deposited)).thenReturn(true);

        workflowService.deposit(1, regularActor);

        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Deposited);
    }

    @Test
    public void testClearSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Deposited);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.setStatus(cheque, Cheque.Status.Cleared)).thenReturn(true);

        workflowService.clear(1, regularActor);

        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Cleared);
    }

    @Test
    public void testBounceSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Deposited);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.setStatus(cheque, Cheque.Status.Bounced)).thenReturn(true);

        workflowService.bounce(1, regularActor);

        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Bounced);
    }

    @Test
    public void testCancelSuccess() throws SQLException {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Draft);

        when(chequeService.getById(1)).thenReturn(cheque);
        when(chequeService.setStatus(cheque, Cheque.Status.Cancelled)).thenReturn(true);

        workflowService.cancel(1, regularActor);

        verify(chequeService, times(1)).setStatus(cheque, Cheque.Status.Cancelled);
    }

    @Test
    public void testCancelIllegalState() {
        Cheque cheque = new Cheque(1, "Payee", new java.math.BigDecimal("100.00"), 1, java.time.LocalDate.now());
        cheque.setStatus(Cheque.Status.Cleared); // Cannot cancel if cleared

        when(chequeService.getById(1)).thenReturn(cheque);

        assertThrows(IllegalStateException.class, () -> {
            workflowService.cancel(1, regularActor);
        });
    }
}
