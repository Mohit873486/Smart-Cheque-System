package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.util.AppState;
import com.chequeprint.util.JasperPrintUtil;
import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PrintService {

    private final ChequeDAO chequeDAO;
    private final BankDAO bankDAO;

    public PrintService() {
        this.chequeDAO = new ChequeDAO();
        this.bankDAO = new BankDAO();
    }

    public PrintService(ChequeDAO chequeDAO, BankDAO bankDAO) {
        this.chequeDAO = chequeDAO;
        this.bankDAO = bankDAO;
    }

    public boolean printCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId);
        if (c == null) {
            throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        }
        return printCheque(c);
    }

    public boolean printCheque(Cheque cheque) throws Exception {
        validateBeforePrint(cheque, null);
        Bank bank = resolveBank(cheque);
        boolean printed = JasperPrintUtil.printCheque(cheque, bank);
        if (printed) {
            try {
                chequeDAO.updateStatus(cheque, Cheque.Status.Printed);
            } catch (Exception dbEx) {
                throw new Exception("Printed but failed to update status — cheque may show as unprinted, contact support", dbEx);
            }
        }
        return printed;
    }

    public boolean previewCheque(Cheque cheque) throws Exception {
        validateBeforePrint(cheque, null);
        Bank bank = resolveBank(cheque);
        boolean printed = false;
        try {
            printed = JasperPrintUtil.previewCheque(cheque, bank);
            if (printed) {
                try {
                    chequeDAO.updateStatus(cheque, Cheque.Status.Printed);
                } catch (Exception dbEx) {
                    throw new Exception("Printed but failed to update status — cheque may show as unprinted, contact support", dbEx);
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return printed;
    }

    public List<Cheque> printAllPending() throws SQLException, BatchPrintException {
        List<Cheque> pending = chequeDAO.findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft
                        || c.getStatus() == Cheque.Status.Pending)
                .collect(Collectors.toList());

        java.util.ArrayList<Cheque> successes = new java.util.ArrayList<>();
        java.util.ArrayList<String> failures = new java.util.ArrayList<>();

        for (Cheque c : pending) {
            try {
                Bank bank = resolveBank(c);
                boolean printed = JasperPrintUtil.printCheque(c, bank);
                if (printed) {
                    try {
                        chequeDAO.updateStatus(c, Cheque.Status.Printed);
                        successes.add(c);
                    } catch (Exception dbEx) {
                        failures.add(c.getChequeNo() + ": Printed but failed to update status — cheque may show as unprinted, contact support");
                    }
                } else {
                    failures.add(c.getChequeNo() + ": Print returned false");
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                failures.add(c.getChequeNo() + ": " + errorMsg);
                System.err.println("Print failed for " + c.getChequeNo() + ": " + errorMsg);
            }
        }

        if (!failures.isEmpty()) {
            throw new BatchPrintException("Batch printing completed with failures.", successes, failures);
        }

        return successes;
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

    public boolean printCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return ChequePrintPipeline.execute(cheque, bank, layout, ownerWindow);
    }

    public boolean printNode(Node node, Window ownerWindow) {
        ensurePrinterSelected();
        return FxPrinterService.printNode(node, ownerWindow);
    }

    public boolean isPrinterSelected() {
        Printer p = AppState.getInstance().getSelectedPrinter();
        return p != null && PrinterUtils.isValidPrinter(p);
    }

    public void ensurePrinterSelected() throws IllegalStateException {
        if (!isPrinterSelected()) {
            throw new IllegalStateException("No valid physical printer selected. Please select a printer in Printer Settings before printing.");
        }
    }

    /**
     * Validates all pre-conditions before printing:
     * 1. Printer is selected
     * 2. Printer is not Fax or virtual
     * 3. Template is loaded
     * 4. Cheque data is valid
     */
    public void validateBeforePrint(Cheque cheque, BankTemplateLayout layout) throws IllegalStateException {
        // Condition 1 & 2: Printer is selected and is a valid physical printer
        Printer printer = AppState.getInstance().getSelectedPrinter();
        if (printer == null) {
            throw new IllegalStateException("No printer selected. Please select a valid printer in Printer Settings before printing.");
        }

        if (!PrinterUtils.isValidPrinter(printer)) {
            String printerName = printer.getName() != null ? printer.getName() : "Unknown";
            throw new IllegalStateException("Selected printer ('" + printerName + "') is invalid. Fax and virtual printers (PDF, XPS, OneNote) are not supported. Please select a physical printer.");
        }

        // Condition 3: Template is loaded
        if (layout == null) {
            layout = AppState.getInstance().getSelectedTemplate();
        }
        if (layout == null) {
            throw new IllegalStateException("No cheque template layout loaded. Please select a bank account with a valid template layout before printing.");
        }

        // Condition 4: Cheque data is valid
        if (cheque == null) {
            throw new IllegalStateException("Invalid cheque data: Cheque object is null.");
        }
        if (cheque.getPayeeName() == null || cheque.getPayeeName().trim().isEmpty()) {
            throw new IllegalStateException("Invalid cheque data: Payee name is required.");
        }
        if (cheque.getAmount() == null || cheque.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid cheque data: Amount must be greater than zero.");
        }
        if (cheque.getIssueDate() == null) {
            throw new IllegalStateException("Invalid cheque data: Issue date is required.");
        }
    }

    public boolean previewInvoice(Invoice invoice) throws Exception {
        return JasperPrintUtil.previewInvoice(invoice);
    }

    public boolean printInvoice(Invoice invoice, Printer printer) throws Exception {
        return JasperPrintUtil.printInvoice(invoice, printer);
    }

    public String exportInvoicePdf(Invoice invoice, String outputDir) throws Exception {
        return JasperPrintUtil.exportInvoicePdf(invoice, outputDir);
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
