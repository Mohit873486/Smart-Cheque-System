package com.chequeprint.service;

import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.JasperPrintUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PrintService {

    private final ChequeDAO chequeDAO = new ChequeDAO();

    // ── Print single cheque by ID ────────────────────────────────────
    public void printCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null)
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        printCheque(c);
    }

    // ── Print single Cheque object ───────────────────────────────────
    public void printCheque(Cheque cheque) throws Exception {
        if (cheque == null)
            throw new IllegalArgumentException("Cheque must not be null.");
        JasperPrintUtil.printCheque(cheque);
        chequeDAO.update(cheque);
    }

    // ── Batch print all Draft / Pending cheques ──────────────────────
    public List<Cheque> printAllPending() throws SQLException {
        List<Cheque> pending = chequeDAO.findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft
                          || c.getStatus() == Cheque.Status.Pending)
                .collect(Collectors.toList());

        for (Cheque c : pending) {
            try {
                JasperPrintUtil.printCheque(c);
                chequeDAO.update(c);
            } catch (Exception e) {
                System.err.println("Print failed for " + c.getChequeNo()
                        + ": " + e.getMessage());
            }
        }
        return pending;
    }

    // ── Count cheques in print queue ─────────────────────────────────
    public int getPrintQueueSize() throws SQLException {
        return (int) chequeDAO.findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft
                          || c.getStatus() == Cheque.Status.Pending)
                .count();
    }

    // ── Cancel a cheque ──────────────────────────────────────────────
    public boolean cancelPrint(int chequeId) throws SQLException {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) return false;
        c.setStatus(Cheque.Status.Cancelled);
        return chequeDAO.update(c);
    }

    // ── Re-print a previously printed cheque ────────────────────────
    public void reprintCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null)
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        JasperPrintUtil.printCheque(c);
        chequeDAO.update(c);
    }

    // ── Export cheque as PDF file ────────────────────────────────────
    public String exportChequePdf(int chequeId, String outputDir) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null)
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        return JasperPrintUtil.exportChequePdf(c, outputDir);
    }
}