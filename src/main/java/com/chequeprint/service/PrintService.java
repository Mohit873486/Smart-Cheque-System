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
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PrintService {

    private static final Logger LOGGER = Logger.getLogger(PrintService.class.getName());

    private final ChequeDAO chequeDAO;
    private final BankDAO bankDAO;
    private final PrinterService printerService = new PrinterService();

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
        Printer selectedPrinter = printerService.resolvePrinterForMode(PrinterService.PrinterRoutingMode.SINGLE)
                .orElseThrow(() -> new IllegalStateException("No default printer available for single cheque printing."));
        Bank bank = resolveBank(cheque);
        boolean printed = JasperPrintUtil.printCheque(cheque, bank, selectedPrinter);
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

        Printer bulkPrinter = printerService.resolvePrinterForMode(PrinterService.PrinterRoutingMode.BULK)
                .orElseThrow(() -> new IllegalStateException("No office printer available for bulk cheque printing."));

        for (Cheque c : pending) {
            try {
                Bank bank = resolveBank(c);
                boolean printed = JasperPrintUtil.printCheque(c, bank, bulkPrinter);
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

    public boolean executeInteractivePrintWorkflow(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        // Step 1: Validate Data
        Cheque validCheque = step2LoadChequeData(cheque);
        Bank activeBank = bank != null ? bank : AppState.getInstance().getSelectedBank();
        BankTemplateLayout validLayout = step1LoadTemplate(validCheque, activeBank, layout);

        logPrePrintDiagnostics(validCheque, validLayout);

        // Step 2: Generate Preview
        javafx.scene.layout.Pane canvas = new javafx.scene.layout.Pane();
        canvas.setPrefSize(validLayout.getWidthInches() * 72.0, validLayout.getHeightInches() * 72.0);
        com.chequeprint.engine.ChequeRenderEngine.renderCheque(canvas, validCheque, activeBank, validLayout);

        // Step 3 & 4: Show Preview Window & User Confirmation
        com.chequeprint.printpreview.PrintPreviewService previewService = new com.chequeprint.printpreview.PrintPreviewService();
        boolean userConfirmed = previewService.previewCheque(validCheque, activeBank, validLayout);
        if (!userConfirmed) {
            return false;
        }

        // Step 5 & 6: Validate Printer & Send to Printer
        validatePrinter();
        return printRenderedCheque(canvas, ownerWindow);
    }

    public boolean executeProfessionalPrintFlow(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return executeInteractivePrintWorkflow(cheque, bank, layout, ownerWindow);
    }

    public boolean printCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return executeInteractivePrintWorkflow(cheque, bank, layout, ownerWindow);
    }

    public boolean printRenderedCheque(Node node, Window ownerWindow) {
        Printer printer = resolvePrinterForPrint();
        return printNode(node, ownerWindow, printer);
    }

    public boolean printNode(Node node, Window ownerWindow) {
        return printRenderedCheque(node, ownerWindow);
    }

    public boolean printNode(Node node, Window ownerWindow, Printer printer) {
        Objects.requireNonNull(node, "Printable node must not be null.");
        validatePrinter(printer);
        AppState.getInstance().setSelectedPrinter(printer);
        LOGGER.info(() -> "Starting print job on printer: " + printer.getName());
        boolean printed = FxPrinterService.printNode(node, ownerWindow, printer);
        if (!printed) {
            LOGGER.warning("Print job failed or was cancelled on printer: " + printer.getName());
        }
        return printed;
    }

    public boolean isPrinterSelected() {
        Printer p = printerService.resolveSelectedOrDefaultPrinter().orElse(null);
        return p != null && PrinterUtils.isValidPrinter(p);
    }

    public void validatePrinter() throws IllegalStateException {
        Printer p = printerService.resolveSelectedOrDefaultPrinter().orElse(null);
        if (p == null) {
            throw new IllegalStateException("No printer selected and no default printer is available. Please select a printer in Printer Settings before printing.");
        }
        if (!PrinterUtils.isValidPrinter(p)) {
            String name = p.getName() != null ? p.getName() : "Unknown";
            throw new IllegalStateException("Selected printer ('" + name + "') is not available.");
        }
    }

    public void validatePrinter(Printer printer) throws IllegalStateException {
        if (printer == null) {
            throw new IllegalStateException("No printer selected. Please select a printer before printing.");
        }
        if (!PrinterUtils.isValidPrinter(printer)) {
            String name = printer.getName() != null ? printer.getName() : "Unknown";
            throw new IllegalStateException("Selected printer ('" + name + "') is not available.");
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

    public List<Printer> getAvailablePrinters() {
        return PrinterUtils.getAllAvailablePrinters();
    }

    public List<String> getAvailablePrinterNames() {
        return PrinterUtils.getValidPrinterNames();
    }

    public Optional<Printer> getSelectedPrinter() {
        Printer printer = printerService.resolveSelectedOrDefaultPrinter().orElse(null);
        return PrinterUtils.isValidPrinter(printer) ? Optional.of(printer) : Optional.empty();
    }

    public Printer selectPrinter(Printer printer) {
        validatePrinter(printer);
        printerService.selectPrinter(printer);
        LOGGER.info(() -> "Selected printer: " + printer.getName());
        return printer;
    }

    public Printer selectPrinterByName(String printerName) {
        return printerService.selectPrinterByName(printerName)
                .orElseThrow(() -> new IllegalStateException("Printer not found: " + printerName));
    }

    public Printer initializeDefaultPrinter() {
        LOGGER.info("Printer initialization requested; restoring saved default printer.");
        return printerService.initializeSelectedPrinter().orElse(null);
    }

    public void setAsDefaultPrinter(Printer printer) {
        printerService.saveDefaultPrinter(printer);
    }

    public boolean testPrint(Window ownerWindow, Printer printer) {
        validatePrinter(printer);

        VBox page = new VBox(8);
        page.setPrefSize(420, 220);
        page.setStyle("-fx-padding: 24; -fx-background-color: white; -fx-border-color: #111827; -fx-border-width: 1;");

        Label title = new Label("Smart Cheque System - Test Print");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label printerLabel = new Label("Printer: " + printer.getName());
        printerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        Label status = new Label("If you can read this page, JavaFX printing is configured correctly.");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        page.getChildren().addAll(title, printerLabel, status);

        try {
            return printNode(page, ownerWindow, printer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Test print failed on printer: " + printer.getName(), e);
            return false;
        }
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

    private Printer resolvePrinterForPrint() {
        Printer printer = printerService.resolveSelectedOrDefaultPrinter().orElse(null);
        validatePrinter(printer);
        return printer;
    }
}
