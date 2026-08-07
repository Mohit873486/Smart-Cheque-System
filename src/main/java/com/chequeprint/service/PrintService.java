package com.chequeprint.service;

import com.chequeprint.model.PageRequest;
import com.chequeprint.dao.BankDAO;
import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import com.chequeprint.state.AppState;
import com.chequeprint.util.JasperPrintUtil;
import com.chequeprint.util.PrinterUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * PrintService — unified print façade for the JavaFX client.
 *
 * Consolidates the responsibilities previously spread across five classes:
 *
 *   • PrintService          — cheque/invoice print orchestration  (was: PrintService)
 *   • PrinterService        — printer discovery, routing, AppState (was: PrinterService)
 *   • PrinterSelectionService — UI-facing printer selection helper  (was: PrinterSelectionService)
 *   • PrinterErrorHandler   — failure classification and messaging  (was: PrinterErrorHandler)
 *   • ChequePrintPipeline   — step-by-step pipeline wrapper         (was: ChequePrintPipeline)
 *
 * Public API is fully backward-compatible. External callers that used any of
 * the five classes require no changes — those classes now delegate here.
 *
 * <h3>Printer Routing</h3>
 * <ul>
 *   <li>SINGLE mode  → default printer → selected printer</li>
 *   <li>BULK mode    → office printer  → default printer → selected printer</li>
 * </ul>
 */
public class PrintService {

    private static final Logger LOGGER = Logger.getLogger(PrintService.class.getName());

    // ── Printer routing enums (inlined from PrinterService) ───────────────────

    public enum PrinterRoutingMode { SINGLE, BULK }
    public enum PrinterType        { DEFAULT, OFFICE }

    // ── Failure type enum (inlined from PrinterErrorHandler) ─────────────────

    public enum PrintFailureType {
        PRINTER_NOT_FOUND,
        PRINTER_OFFLINE,
        JOB_FAILED,
        UNKNOWN
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final ChequeDAO chequeDAO;
    private final BankDAO   bankDAO;
    private final AppState  appState;

    /** Per-instance printer-type → name configuration (OFFICE, DEFAULT). */
    private final Map<PrinterType, String> printerTypeConfig = new EnumMap<>(PrinterType.class);

    // ── Constructors ──────────────────────────────────────────────────────────

    public PrintService() {
        this(new ChequeDAO(), new BankDAO(), AppState.getInstance());
    }

    public PrintService(ChequeDAO chequeDAO, BankDAO bankDAO) {
        this(chequeDAO, bankDAO, AppState.getInstance());
    }

    PrintService(ChequeDAO chequeDAO, BankDAO bankDAO, AppState appState) {
        this.chequeDAO = chequeDAO;
        this.bankDAO   = bankDAO;
        this.appState  = appState;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. CHEQUE PRINTING
    // ═════════════════════════════════════════════════════════════════════════

    /** Looks up the cheque by ID, then prints it. */
    public boolean printCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId).orElse(null);
        if (c == null) throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        return printCheque(c);
    }

    /** Prints a single cheque on the default/selected printer and marks it Printed. */
    public boolean printCheque(Cheque cheque) throws Exception {
        validateBeforePrint(cheque, null);
        Printer printer = resolvePrinterForMode(PrinterRoutingMode.SINGLE)
                .orElseThrow(() -> new IllegalStateException("No printer available for single cheque printing."));
        Bank bank = resolveBank(cheque);
        boolean ok = JasperPrintUtil.printCheque(cheque, bank, printer);
        if (ok) updateStatusOrThrow(cheque, Cheque.Status.Printed);
        return ok;
    }

    /**
     * Renders a preview and marks the cheque Printed on confirmation.
     * Delegates to JasperPrintUtil for the preview window.
     */
    public boolean previewCheque(Cheque cheque) throws Exception {
        validateBeforePrint(cheque, null);
        Bank bank = resolveBank(cheque);
        boolean ok = JasperPrintUtil.previewCheque(cheque, bank);
        if (ok) updateStatusOrThrow(cheque, Cheque.Status.Printed);
        return ok;
    }

    /** Interactive print workflow: validate → render → preview dialog → print. */
    public boolean printCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return executeInteractivePrintWorkflow(cheque, bank, layout, ownerWindow);
    }

    /** @see #printCheque(Cheque, Bank, BankTemplateLayout, Window) */
    public boolean executeProfessionalPrintFlow(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return executeInteractivePrintWorkflow(cheque, bank, layout, ownerWindow);
    }

    public boolean executeInteractivePrintWorkflow(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        Cheque   validCheque = step2LoadChequeData(cheque);
        Bank     activeBank  = bank != null ? bank : appState.getSelectedBank();
        BankTemplateLayout validLayout = step1LoadTemplate(validCheque, activeBank, layout);

        logPrePrintDiagnostics(validCheque, validLayout);

        Pane canvas = new Pane();
        canvas.setPrefSize(validLayout.getWidthInches() * 72.0, validLayout.getHeightInches() * 72.0);
        com.chequeprint.engine.ChequeRenderEngine.renderCheque(canvas, validCheque, activeBank, validLayout);

        com.chequeprint.printpreview.PrintPreviewService previewService = new com.chequeprint.printpreview.PrintPreviewService();
        boolean confirmed = previewService.previewCheque(validCheque, activeBank, validLayout);
        if (!confirmed) return false;

        validatePrinter();
        return printRenderedCheque(canvas, ownerWindow);
    }

    /** Batch-prints all Draft/Pending cheques; throws BatchPrintException if any fail. */
        public List<Cheque> printAllPending() throws SQLException, BatchPrintException {
        List<Cheque> pending = chequeDAO.findAll(PageRequest.of(0, 1000)).getContent().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft || c.getStatus() == Cheque.Status.Pending)
                .collect(Collectors.toList());

        List<Cheque> successes = new ArrayList<>();
        List<String> failures  = new ArrayList<>();

        Printer bulkPrinter = resolvePrinterForMode(PrinterRoutingMode.BULK)
                .orElseThrow(() -> new IllegalStateException("No printer available for bulk cheque printing."));

        for (Cheque c : pending) {
            try {
                Bank bank = resolveBank(c);
                boolean ok = JasperPrintUtil.printCheque(c, bank, bulkPrinter);
                if (ok) {
                    chequeDAO.updateStatus(c.getId(), Cheque.Status.Printed);   // ← FIX: c → c.getId()
                    successes.add(c);
                } else {
                    failures.add(c.getChequeNo() + ": Print returned false");
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                failures.add(c.getChequeNo() + ": " + msg);
                LOGGER.warning("Batch print failed for " + c.getChequeNo() + ": " + msg);
            }
        }

        if (!failures.isEmpty()) throw new BatchPrintException("Batch printing completed with failures.", successes, failures);
        return successes;
    }

    public void reprintCheque(int chequeId) throws Exception {
        Cheque c = chequeDAO.findById(chequeId).orElse(null);
        if (c == null) throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        Bank bank = resolveBank(c);
        if (JasperPrintUtil.printCheque(c, bank)) chequeDAO.updateStatus(c.getId(), Cheque.Status.Printed);
    }

            public int getPrintQueueSize() throws SQLException {
        return (int) chequeDAO.findAll(PageRequest.of(0, 1000)).getContent().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Draft || c.getStatus() == Cheque.Status.Pending)
                .count();
    }

    public boolean cancelPrint(int chequeId) throws SQLException {
        Cheque c = chequeDAO.findById(chequeId).orElse(null);
        if (c == null) return false;
        c.setStatus(Cheque.Status.Cancelled);
        return chequeDAO.update(c).isOk();   // ← FIX: ApiResponse<Void> → boolean via .isOk()
    }
    // ═════════════════════════════════════════════════════════════════════════
    // 2. PDF EXPORT
    // ═════════════════════════════════════════════════════════════════════════
    public String exportChequePdf(int chequeId, String outputDir) throws Exception {
        Cheque c = chequeDAO.findById(chequeId).orElse(null);
        if (c == null) throw new IllegalArgumentException("Cheque not found: id=" + chequeId);
        return JasperPrintUtil.exportChequePdf(c, outputDir, resolveBank(c));
    }

    public String exportSelectedChequePdfAndMarkPrinted(Cheque cheque, String outputDir) throws Exception {
        Objects.requireNonNull(cheque, "Cheque must not be null.");
        String path = JasperPrintUtil.exportChequePdf(cheque, outputDir, resolveBank(cheque));
        chequeDAO.updateStatus(cheque.getId(), Cheque.Status.Printed);   // ← FIX: cheque → cheque.getId()
        return path;
    }
    // ═════════════════════════════════════════════════════════════════════════
    // 3. INVOICE OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    public boolean previewInvoice(Invoice invoice)                          throws Exception { return JasperPrintUtil.previewInvoice(invoice); }
    public boolean printInvoice  (Invoice invoice, Printer printer)         throws Exception { return JasperPrintUtil.printInvoice(invoice, printer); }
    public String  exportInvoicePdf(Invoice invoice, String outputDir)      throws Exception { return JasperPrintUtil.exportInvoicePdf(invoice, outputDir); }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. NODE / RAW PRINTING
    // ═════════════════════════════════════════════════════════════════════════

    public boolean printRenderedCheque(Node node, Window ownerWindow) {
        return printNode(node, ownerWindow, resolvePrinterForPrint());
    }

    public boolean printNode(Node node, Window ownerWindow) {
        return printRenderedCheque(node, ownerWindow);
    }

    public boolean printNode(Node node, Window ownerWindow, Printer printer) {
        Objects.requireNonNull(node, "Printable node must not be null.");
        validatePrinter(printer);
        appState.setSelectedPrinter(printer);
        LOGGER.info(() -> "Starting print job on: " + printer.getName());
        boolean ok = FxPrinterService.printNode(node, ownerWindow, printer);
        if (!ok) LOGGER.warning("Print job failed or was cancelled on: " + printer.getName());
        return ok;
    }

    /** Produces a test-print page to verify the printer is working. */
    public boolean testPrint(Window ownerWindow, Printer printer) {
        validatePrinter(printer);
        VBox page = new VBox(8);
        page.setPrefSize(420, 220);
        page.setStyle("-fx-padding: 24; -fx-background-color: white; -fx-border-color: #111827; -fx-border-width: 1;");
        page.getChildren().addAll(
                styledLabel("Smart Cheque System — Test Print", "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;"),
                styledLabel("Printer: " + printer.getName(),   "-fx-font-size: 12px; -fx-text-fill: #374151;"),
                styledLabel("If you can read this, JavaFX printing is configured correctly.", "-fx-font-size: 12px; -fx-text-fill: #374151;")
        );
        try {
            return printNode(page, ownerWindow, printer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Test print failed on: " + printer.getName(), e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. PRINTER DISCOVERY & SELECTION  (formerly PrinterService + PrinterSelectionService)
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns all usable JavaFX printers. */
    public List<Printer> getAvailablePrinters() {
        List<Printer> list = new ArrayList<>();
        try {
            for (Printer p : Printer.getAllPrinters()) {
                if (isUsablePrinter(p)) list.add(p);
            }
        } catch (Throwable t) {
            LOGGER.warning("Failed to query JavaFX printers: " + t.getMessage());
        }
        return list;
    }

    /** Refreshes the printer list in AppState and returns it as an ObservableList. */
    public ObservableList<Printer> refreshPrinters() {
        List<Printer> printers = getAvailablePrinters();
        appState.getAvailablePrinters().setAll(printers);
        Printer sel = appState.getSelectedPrinter();
        if (sel != null && findByName(printers, sel.getName()).isEmpty()) appState.setSelectedPrinter(null);
        return appState.getAvailablePrinters();
    }

    public ObservableList<String> refreshPrinterNames() {
        ObservableList<String> names = FXCollections.observableArrayList();
        refreshPrinters().forEach(p -> names.add(p.getName()));
        return names;
    }

    public List<String> getAvailablePrinterNames() {
        return getAvailablePrinters().stream().map(Printer::getName).collect(Collectors.toList());
    }

    public Optional<Printer> findPrinterByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return findByName(refreshPrinters(), name);
    }

    public Optional<Printer> getSelectedPrinter() {
        Printer p = appState.getSelectedPrinter();
        if (p == null) return Optional.empty();
        return findByName(Collections.unmodifiableList(appState.getAvailablePrinters()), p.getName());
    }

    public Optional<Printer> getDefaultPrinter() {
        Printer p = appState.getDefaultPrinter();
        if (p == null) return Optional.empty();
        return findByName(refreshPrinters(), p.getName());
    }

    public Optional<Printer> resolveSelectedOrDefaultPrinter() {
        refreshPrinters();
        Printer p = appState.resolveSelectedOrDefaultPrinter();
        if (p == null) return Optional.empty();
        return findByName(appState.getAvailablePrinters(), p.getName());
    }

    public Printer selectPrinter(Printer printer) {
        if (!isUsablePrinter(printer)) throw new IllegalArgumentException("Printer is invalid or unavailable.");
        Printer found = findByName(refreshPrinters(), printer.getName())
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printer.getName()));
        appState.setSelectedPrinter(found);
        LOGGER.info(() -> "Selected printer: " + found.getName());
        return found;
    }

    public Optional<Printer> selectPrinterByName(String name) {
        Optional<Printer> p = findPrinterByName(name);
        p.ifPresent(appState::setSelectedPrinter);
        return p;
    }

    public Printer selectPrinterByNameOrThrow(String name) {
        return selectPrinterByName(name)
                .orElseThrow(() -> new IllegalStateException("Printer not found: " + name));
    }

    public Printer saveDefaultPrinter(Printer printer) {
        Printer selected = selectPrinter(printer);
        appState.setDefaultPrinter(selected);
        return selected;
    }

    public Printer initializeDefaultPrinter() {
        ObservableList<Printer> printers = refreshPrinters();
        if (printers.isEmpty()) { appState.setSelectedPrinter(null); return null; }
        Optional<Printer> sel = getSelectedPrinter();
        if (sel.isPresent()) return sel.get();
        Optional<Printer> def = getDefaultPrinter();
        def.ifPresent(appState::setSelectedPrinter);
        return def.orElse(null);
    }

    public void setAsDefaultPrinter(Printer printer) { saveDefaultPrinter(printer); }

    public boolean isPrinterSelected() {
        return resolveSelectedOrDefaultPrinter().filter(PrinterUtils::isValidPrinter).isPresent();
    }

    public boolean hasPrinters() { return !appState.getAvailablePrinters().isEmpty(); }

    /** Configures a named printer for a routing slot (DEFAULT or OFFICE). */
    public PrintService configurePrinterType(PrinterType type, String name) {
        Objects.requireNonNull(type, "PrinterType must not be null.");
        if (name == null || name.isBlank()) printerTypeConfig.remove(type);
        else printerTypeConfig.put(type, name.trim());
        return this;
    }

    public PrintService setOfficePrinter (String name) { return configurePrinterType(PrinterType.OFFICE,  name); }
    public PrintService setDefaultPrinter(String name) { return configurePrinterType(PrinterType.DEFAULT, name); }

    public Optional<Printer> getConfiguredPrinter(PrinterType type) {
        String name = printerTypeConfig.get(type);
        if (name == null || name.isBlank()) return Optional.empty();
        return findPrinterByName(name);
    }

    /**
     * Routes to the appropriate printer for the given mode:
     * BULK → office → default → selected; SINGLE → default → selected.
     */
    public Optional<Printer> resolvePrinterForMode(PrinterRoutingMode mode) {
        refreshPrinters();
        Optional<Printer> resolved;
        if (mode == PrinterRoutingMode.BULK) {
            resolved = getConfiguredPrinter(PrinterType.OFFICE)
                    .or(this::getDefaultPrinter)
                    .or(this::resolveSelectedOrDefaultPrinter);
            resolved.ifPresent(p -> LOGGER.info("[SmartRouting] BULK → " + p.getName()));
        } else {
            resolved = getDefaultPrinter().or(this::resolveSelectedOrDefaultPrinter);
            resolved.ifPresent(p -> LOGGER.info("[SmartRouting] SINGLE → " + p.getName()));
        }
        return resolved;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 6. VALIDATION  (formerly split between PrintService + PrinterErrorHandler)
    // ═════════════════════════════════════════════════════════════════════════

    public void validatePrinter() {
        Printer p = resolveSelectedOrDefaultPrinter().orElse(null);
        if (p == null) throw new IllegalStateException("No printer selected and no default printer is available.");
        if (!PrinterUtils.isValidPrinter(p))
            throw new IllegalStateException("Printer '" + p.getName() + "' is not available.");
    }

    public void validatePrinter(Printer printer) {
        if (printer == null) throw new IllegalStateException("No printer selected.");
        if (!PrinterUtils.isValidPrinter(printer))
            throw new IllegalStateException("Printer '" + printer.getName() + "' is not available.");
    }

    public void validateBeforePrint(Cheque cheque, BankTemplateLayout layout) {
        step1LoadTemplate(cheque, null, layout);
        step2LoadChequeData(cheque);
        validatePrinter();
    }

    public void ensurePrinterSelected() { validatePrinter(); }

    // ── Error classification (formerly PrinterErrorHandler) ──────────────────

    /** Classifies a print failure into a {@link PrintFailureType}. */
    public static PrintFailureType classifyFailure(Printer printer, Throwable cause, boolean jobFailed) {
        if (printer == null || !PrinterUtils.isValidPrinter(printer)) return PrintFailureType.PRINTER_NOT_FOUND;
        if (jobFailed) return PrintFailureType.JOB_FAILED;
        String msg = cause != null ? cause.getMessage() : null;
        if (msg != null && isPrinterOfflineMessage(msg)) return PrintFailureType.PRINTER_OFFLINE;
        return PrintFailureType.UNKNOWN;
    }

    public static PrintFailureType classifyFailure(Printer printer, Throwable cause) {
        return classifyFailure(printer, cause, false);
    }

    /** Builds a user-friendly error message and logs the failure. */
    public static String buildPrinterErrorMessage(Printer printer, String action, Throwable cause, boolean jobFailed) {
        PrintFailureType type = classifyFailure(printer, cause, jobFailed);
        String name = printer != null ? printer.getName() : "<Unknown>";
        String msg = switch (type) {
            case PRINTER_NOT_FOUND -> "❌ Printer Not Found: '" + name + "' is not installed. Please select a valid printer.";
            case PRINTER_OFFLINE   -> "🔌 Printer Offline: '" + name + "' appears to be offline. Check power and connection.";
            case JOB_FAILED        -> "⚠️ Print Job Failed: Job sent to '" + name + "' failed to spool. Retry or restart the spooler.";
            default                -> "⚠️ Printing Error: '" + action + "' on '" + name + "' failed. " + safeMessage(cause);
        };
        LOGGER.log(cause != null ? Level.SEVERE : Level.WARNING,
                "[PrintFailure] action='" + action + "' printer='" + name + "' type=" + type +
                        (cause != null ? " error=" + cause.getMessage() : ""), cause);
        return msg;
    }

    public static String buildPrinterErrorMessage(Printer printer, String action, Throwable cause) {
        return buildPrinterErrorMessage(printer, action, cause, false);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 7. PIPELINE STEPS  (formerly ChequePrintPipeline)
    // ═════════════════════════════════════════════════════════════════════════

    public BankTemplateLayout step1LoadTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        if (layout != null) return layout;
        Bank active = bank != null ? bank : appState.getSelectedBank();
        BankTemplateLayout resolved = new TemplateService().getTemplateForBank(active);
        if (resolved == null) resolved = appState.getSelectedTemplate();
        if (resolved == null)
            throw new IllegalStateException("No cheque template layout loaded. Please select a bank layout before printing.");
        return resolved;
    }

    public Cheque step2LoadChequeData(Cheque cheque) {
        Cheque active = cheque != null ? cheque : appState.getCurrentCheque();
        return new ChequeService().validateChequeData(active);
    }

    public void logPrePrintDiagnostics(Cheque cheque, BankTemplateLayout layout) {
        Printer p = appState.getSelectedPrinter();
        boolean validCheque = cheque != null
                && cheque.getPayeeName() != null && !cheque.getPayeeName().isBlank()
                && cheque.getAmount() != null && cheque.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                && cheque.getIssueDate() != null;

        LOGGER.info(() -> "\n==========================================================\n"
                + "[PRINT DIAGNOSTICS]\n"
                + "• Printer  : " + (p != null ? p.getName() : "None") + "\n"
                + "• Template : " + (layout != null
                        ? layout.getWidthInches() + "\" × " + layout.getHeightInches() + "\""
                        : "Missing") + "\n"
                + "• Cheque   : " + (validCheque ? "Valid" : "Invalid/Incomplete") + "\n"
                + "==========================================================");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Bank resolveBank(Cheque cheque) {
        if (cheque == null || cheque.getBankId() <= 0) return null;
        try { return bankDAO.findById(cheque.getBankId()); } catch (Exception e) { return null; }
    }

    private Printer resolvePrinterForPrint() {
        Printer p = resolveSelectedOrDefaultPrinter().orElse(null);
        validatePrinter(p);
        return p;
    }

    private void updateStatusOrThrow(Cheque cheque, Cheque.Status status) throws Exception {
        try {
            chequeDAO.updateStatus(cheque.getId(), status);   // ← FIX: cheque → cheque.getId()
        } catch (Exception e) {
            throw new Exception("Printed but failed to update status — contact support.", e);
        }
    }
    
    private Optional<Printer> findByName(List<Printer> printers, String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return printers.stream().filter(p -> p.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    private static boolean isUsablePrinter(Printer p) {
        return p != null && p.getName() != null && !p.getName().isBlank();
    }

    private static boolean isPrinterOfflineMessage(String msg) {
        String low = msg.toLowerCase();
        return Arrays.asList("offline", "not connected", "disconnected", "unavailable",
                "not available", "paper jam", "out of paper", "spooler error")
                .stream().anyMatch(low::contains);
    }

    private static String safeMessage(Throwable t) {
        if (t == null) return "No details available.";
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private static Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }
}
