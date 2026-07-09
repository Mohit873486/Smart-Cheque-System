package com.chequeprint.service;

import com.chequeprint.model.AuditAction;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;

import java.sql.SQLException;

public class ChequeWorkflowService {

  private final ChequeService chequeService;
  private final PrintService printService;
  private final AuditService auditService;

  public ChequeWorkflowService() {
    this.chequeService = new ChequeService();
    this.printService = new PrintService();
    this.auditService = new AuditService();
  }

  public ChequeWorkflowService(ChequeService chequeService, PrintService printService, AuditService auditService) {
    this.chequeService = chequeService;
    this.printService = printService;
    this.auditService = auditService;
  }

  public Cheque createPending(Cheque cheque, User actor) throws SQLException {
    AccessControl.requirePermission(actor, Permission.CREATE_CHEQUE);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque cannot be null.");
    }
    cheque.setStatus(Cheque.Status.Pending);
    if (!chequeService.save(cheque)) {
      throw new SQLException("Failed to create pending cheque.");
    }
    auditService.record(actor, "cheques", cheque.getId(), AuditAction.INSERT,
        "Created cheque pending approval: " + cheque.getChequeNo());
    return cheque;
  }

  public void approve(int chequeId, User actor) throws SQLException {
    AccessControl.requirePermission(actor, Permission.APPROVE_CHEQUE);
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    if (cheque.getStatus() != Cheque.Status.Pending) {
      throw new IllegalStateException("Only pending cheques can be approved.");
    }
    boolean updated = chequeService.approveCheque(chequeId);
    if (!updated) {
      throw new SQLException("Could not approve cheque: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.APPROVE,
        "Approved cheque: " + cheque.getChequeNo());
  }

  public void reject(int chequeId, User actor) throws SQLException {
    AccessControl.requirePermission(actor, Permission.REJECT_CHEQUE);
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    if (cheque.getStatus() != Cheque.Status.Pending) {
      throw new IllegalStateException("Only pending cheques can be rejected.");
    }
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Rejected);
    if (!updated) {
      throw new SQLException("Could not reject cheque: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.REJECT,
        "Rejected cheque: " + cheque.getChequeNo());
  }

  public void print(int chequeId, User actor) throws Exception {
    AccessControl.requirePermission(actor, Permission.PRINT_CHEQUE);
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }

    if (cheque.getStatus() != Cheque.Status.Approved
        && cheque.getStatus() != Cheque.Status.Printed) {
      String statusMsg = "Current status: " + cheque.getStatus().name();
      throw new IllegalStateException("Cheque must be approved before printing. " + statusMsg);
    }

    boolean printed = false;
    String printerName = "Unknown";
    String errorMessage = null;

    try {
        com.chequeprint.util.JasperPrintUtil.setLastUsedPrinterName("Default Printer");

        printed = printService.previewCheque(cheque);
        if (!printed) {
            errorMessage = "Preview closed or print cancelled.";
            throw new IllegalStateException("Printing was cancelled or failed.");
        }

        printerName = com.chequeprint.util.JasperPrintUtil.getLastUsedPrinterName();
        chequeService.setStatus(cheque, Cheque.Status.Printed);

    } catch (Exception e) {
        errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        printerName = com.chequeprint.util.JasperPrintUtil.getLastUsedPrinterName();
        throw e;
    } finally {
        String statusText = printed ? "SUCCESS" : "FAILURE";
        String details = String.format("Print attempt on cheque %s. Printer: %s. Status: %s.%s",
            cheque.getChequeNo(),
            printerName,
            statusText,
            (errorMessage != null ? " Error: " + errorMessage : "")
        );
        auditService.record(actor, "cheques", chequeId, AuditAction.PRINT, details);
    }
  }

  public void deposit(int chequeId, User actor) throws SQLException {
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    if (cheque.getStatus() != Cheque.Status.Printed && cheque.getStatus() != Cheque.Status.Approved) {
      throw new IllegalStateException("Only approved or printed cheques can be marked as Deposited. Current status: " + cheque.getStatus());
    }
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Deposited);
    if (!updated) {
      throw new SQLException("Could not mark cheque as Deposited: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.UPDATE,
        "Marked cheque as Deposited: " + cheque.getChequeNo());
  }

  public void clear(int chequeId, User actor) throws SQLException {
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    if (cheque.getStatus() != Cheque.Status.Deposited && cheque.getStatus() != Cheque.Status.Printed) {
      throw new IllegalStateException("Only printed or deposited cheques can be marked as Cleared. Current status: " + cheque.getStatus());
    }
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Cleared);
    if (!updated) {
      throw new SQLException("Could not mark cheque as Cleared: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.UPDATE,
        "Marked cheque as Cleared: " + cheque.getChequeNo());
  }

  public void bounce(int chequeId, User actor) throws SQLException {
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    if (cheque.getStatus() != Cheque.Status.Deposited && cheque.getStatus() != Cheque.Status.Printed) {
      throw new IllegalStateException("Only printed or deposited cheques can be marked as Bounced. Current status: " + cheque.getStatus());
    }
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Bounced);
    if (!updated) {
      throw new SQLException("Could not mark cheque as Bounced: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.UPDATE,
        "Marked cheque as Bounced: " + cheque.getChequeNo());
  }

  public void cancel(int chequeId, User actor) throws SQLException {
    Cheque cheque = chequeService.getById(chequeId);
    if (cheque == null) {
      throw new IllegalArgumentException("Cheque not found: " + chequeId);
    }
    Cheque.Status current = cheque.getStatus();
    if (current == Cheque.Status.Cleared || current == Cheque.Status.Bounced || current == Cheque.Status.Rejected || current == Cheque.Status.Cancelled) {
      throw new IllegalStateException("Cannot cancel a cheque that is in " + current.name() + " status.");
    }
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Cancelled);
    if (!updated) {
      throw new SQLException("Could not cancel cheque: " + cheque.getChequeNo());
    }
    auditService.record(actor, "cheques", chequeId, AuditAction.UPDATE,
        "Cancelled cheque: " + cheque.getChequeNo());
  }
}
