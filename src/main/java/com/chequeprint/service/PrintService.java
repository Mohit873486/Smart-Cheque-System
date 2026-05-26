package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.JasperPrintUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PrintService {

    private final ChequeDAO chequeDAO = new ChequeDAO();
    private final BankDAO bankDAO = new BankDAO();

    public boolean printCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) {
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        }
        return printCheque(c);
    }

    public boolean printCheque(Cheque cheque) throws Exception {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque must not be null.");
        }
        Bank bank = resolveBank(cheque);
        boolean printed = JasperPrintUtil.printCheque(cheque, bank);
        if (printed) {
            chequeDAO.updateStatus(cheque, Cheque.Status.Printed);
        }
        return printed;
    }

    public boolean previewCheque(Cheque cheque) throws Exception {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque must not be null.");
        }
        Bank bank = resolveBank(cheque);
        boolean printed = JasperPrintUtil.previewCheque(cheque, bank);
        if (printed) {
            chequeDAO.updateStatus(cheque, Cheque.Status.Printed);
        }
        return printed;
    }

    public List<Cheque> printAllPending() throws SQLException {
        List<Cheque> pending = chequeDAO.findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft
                        || c.getStatus() == Cheque.Status.Pending)
                .collect(Collectors.toList());

        for (Cheque c : pending) {
            try {
                Bank bank = resolveBank(c);
                boolean printed = JasperPrintUtil.printCheque(c, bank);
                if (printed) {
                    chequeDAO.updateStatus(c, Cheque.Status.Printed);
                }
            } catch (Exception e) {
                System.err.println("Print failed for " + c.getChequeNo()
                        + ": " + e.getMessage());
            }
        }
        return pending;
    }

    public int getPrintQueueSize() throws SQLException {
        return (int) chequeDAO.findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft
                        || c.getStatus() == Cheque.Status.Pending)
                .count();
    }

    public boolean cancelPrint(int chequeId) throws SQLException {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) {
            return false;
        }
        c.setStatus(Cheque.Status.Cancelled);
        return chequeDAO.update(c);
    }

    public void reprintCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) {
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        }
        Bank bank = resolveBank(c);
        boolean printed = JasperPrintUtil.printCheque(c, bank);
        if (printed) {
            chequeDAO.updateStatus(c, Cheque.Status.Printed);
        }
    }

    public String exportChequePdf(int chequeId, String outputDir) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) {
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        }
        Bank bank = resolveBank(c);
        return JasperPrintUtil.exportChequePdf(c, outputDir, bank);
    }

    public String exportSelectedChequePdfAndMarkPrinted(Cheque cheque, String outputDir) throws Exception {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque must not be null.");
        }

        Bank bank = resolveBank(cheque);
        String pdfPath = JasperPrintUtil.exportChequePdf(cheque, outputDir, bank);

        chequeDAO.updateStatus(cheque, Cheque.Status.Printed);

        return pdfPath;
    }

    private Bank resolveBank(Cheque cheque) {
        if (cheque == null || cheque.getBankId() <= 0) {
            return null;
        }
        try {
            return bankDAO.findById(cheque.getBankId());
        } catch (Exception e) {
            return null;
        }
    }
}
