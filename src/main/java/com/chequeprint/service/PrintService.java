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

    public boolean executeProfessionalPrintFlow(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        // Stage 1: Validate Printer
        validatePrinter();

        // Stage 2: Load Template
        BankTemplateLayout finalLayout = step1LoadTemplate(cheque, bank, layout);

        // Stage 3: Load Cheque Data
        Cheque finalCheque = step2LoadChequeData(cheque);

        logPrePrintDiagnostics(finalCheque, finalLayout);

        // Stage 4: Render Cheque
        javafx.scene.layout.Pane canvas = new javafx.scene.layout.Pane();
        canvas.setPrefSize(finalLayout.getWidthInches() * 72.0, finalLayout.getHeightInches() * 72.0);
        com.chequeprint.engine.ChequeRenderEngine.renderCheque(canvas, finalCheque, bank, finalLayout);

        // Stage 5: Show Preview Modal
        com.chequeprint.printpreview.PrintPreviewService previewService = new com.chequeprint.printpreview.PrintPreviewService();
        boolean approved = previewService.previewCheque(finalCheque, bank, finalLayout);
        if (!approved) {
            return false;
        }

        // Stage 6: Send to Selected Printer
        return printRenderedCheque(canvas, ownerWindow);
    }

    public boolean printCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return executeProfessionalPrintFlow(cheque, bank, layout, ownerWindow);
    }

    public boolean printRenderedCheque(Node node, Window ownerWindow) {
        validatePrinter();
        return FxPrinterService.printNode(node, ownerWindow);
    }

    public boolean printNode(Node node, Window ownerWindow) {
        return printRenderedCheque(node, ownerWindow);
    }

    public boolean isPrinterSelected() {
        Printer p = AppState.getInstance().getSelectedPrinter();
        return p != null && PrinterUtils.isValidPrinter(p);
    }

    public void validatePrinter() throws IllegalStateException {
        Printer p = AppState.getInstance().getSelectedPrinter();
        if (p == null) {
            throw new IllegalStateException("No printer selected. Please select a printer in Printer Settings before printing.");
        }
        if (!PrinterUtils.isValidPrinter(p)) {
            String name = p.getName() != null ? p.getName() : "Unknown";
            throw new IllegalStateException("Selected printer ('" + name + "') is invalid. Fax and virtual printers (PDF, XPS, OneNote) are not supported.");
        }
    }

    public void validateBeforePrint(Cheque cheque, BankTemplateLayout layout) throws IllegalStateException {
        step1LoadTemplate(cheque, null, layout);
        step2LoadChequeData(cheque);
        validatePrinter();
    }

    public void ensurePrinterSelected() throws IllegalStateException {
        validatePrinter();
    }

    public BankTemplateLayout step1LoadTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        if (layout != null) {
            return layout;
        }
        Bank activeBank = bank != null ? bank : AppState.getInstance().getSelectedBank();
        TemplateService templateService = new TemplateService();
        BankTemplateLayout resolved = templateService.getTemplateForBank(activeBank);
        if (resolved == null) {
            resolved = AppState.getInstance().getSelectedTemplate();
        }
        if (resolved == null) {
            throw new IllegalStateException("No cheque template layout loaded. Please select a bank layout before printing.");
        }
        return resolved;
    }

    public Cheque step2LoadChequeData(Cheque cheque) {
        Cheque activeCheque = cheque != null ? cheque : AppState.getInstance().getCurrentCheque();
        ChequeService chequeService = new ChequeService();
        return chequeService.validateChequeData(activeCheque);
    }

    public void logPrePrintDiagnostics(Cheque cheque, BankTemplateLayout layout) {
        Printer printer = AppState.getInstance().getSelectedPrinter();
        String printerName = printer != null ? printer.getName() : "None / Not Selected";

        boolean templateLoaded = (layout != null);
        String templateInfo = templateLoaded
                ? "Yes (Dimensions: " + layout.getWidthInches() + "\" x " + layout.getHeightInches() + "\")"
                : "No (Missing layout)";

        boolean validCheque = (cheque != null
                && cheque.getPayeeName() != null && !cheque.getPayeeName().trim().isEmpty()
                && cheque.getAmount() != null && cheque.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                && cheque.getIssueDate() != null);
        String chequeStatus = validCheque
                ? "Valid (Payee='" + cheque.getPayeeName() + "', Amount=" + cheque.getAmount() + ", Date=" + cheque.getIssueDate() + ")"
                : "Invalid / Incomplete";

        System.out.println("==========================================================");
        System.out.println("[PRINT DIAGNOSTICS LOG]");
        System.out.println("• Selected Printer Name : " + printerName);
        System.out.println("• Template Loaded       : " + templateInfo);
        System.out.println("• Cheque Data Status    : " + chequeStatus);
        System.out.println("==========================================================");
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
