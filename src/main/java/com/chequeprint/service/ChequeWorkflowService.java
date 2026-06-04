package com.chequeprint.service;

import com.chequeprint.model.AuditAction;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;

import java.sql.SQLException;

public class ChequeWorkflowService {

  private final ChequeService chequeService = new ChequeService();
  private final PrintService printService = new PrintService();
  private final AuditService auditService = new AuditService();

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
    boolean updated = chequeService.setStatus(cheque, Cheque.Status.Approved);
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

    boolean printed = printService.previewCheque(cheque);
    if (!printed) {
      throw new IllegalStateException("Printing was cancelled or failed.");
    }

    // Update status to Printed after successful preview
    chequeService.setStatus(cheque, Cheque.Status.Printed);

    auditService.record(actor, "cheques", chequeId, AuditAction.PRINT,
        "Printed cheque: " + cheque.getChequeNo());
  }
}
