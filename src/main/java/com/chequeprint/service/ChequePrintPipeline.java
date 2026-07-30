package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import com.chequeprint.util.ChequePreviewEngine;
import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import java.math.BigDecimal;

/**
 * Structured 5-Step Cheque Print Pipeline:
 * Step 1: Load template
 * Step 2: Load cheque data
 * Step 3: Merge data into template
 * Step 4: Render final cheque layout
 * Step 5: Send to selected printer
 *
 * Ensures no step is skipped in the print workflow.
 */
public final class ChequePrintPipeline {

    private ChequePrintPipeline() {
        // Utility class
    }

    public static boolean execute(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        System.out.println("[PrintPipeline] === STARTING 5-STEP PRINT PIPELINE ===");

        // STEP 1: Load template
        BankTemplateLayout finalLayout = step1LoadTemplate(cheque, bank, layout);

        // STEP 2: Load cheque data
        Cheque finalCheque = step2LoadChequeData(cheque);

        // Pre-print Diagnostic Log
        logPrePrintDiagnostics(finalCheque, finalLayout);

        // STEP 3: Merge data into template
        BankTemplateLayout mergedLayout = step3MergeDataIntoTemplate(finalCheque, bank, finalLayout);

        // STEP 4: Render final cheque layout
        Node renderedCanvas = step4RenderFinalChequeLayout(finalCheque, bank, mergedLayout);

        // STEP 5: Send to selected printer
        return step5SendToSelectedPrinter(renderedCanvas, ownerWindow);
    }

    /**
     * Logs comprehensive pre-print diagnostic summary:
     * - Selected Printer Name
     * - Template Loaded (Yes/No)
     * - Cheque Data Status
     * - Coordinates Applied
     */
    public static void logPrePrintDiagnostics(Cheque cheque, BankTemplateLayout layout) {
        Printer printer = AppState.getInstance().getSelectedPrinter();
        String printerName = printer != null ? printer.getName() : "None / Not Selected";

        boolean templateLoaded = (layout != null);
        String templateInfo = templateLoaded
                ? "Yes (Dimensions: " + layout.getWidthInches() + "\" x " + layout.getHeightInches() + "\")"
                : "No (Missing layout)";

        boolean validCheque = (cheque != null
                && cheque.getPayeeName() != null && !cheque.getPayeeName().trim().isEmpty()
                && cheque.getAmount() != null && cheque.getAmount().compareTo(BigDecimal.ZERO) > 0
                && cheque.getIssueDate() != null);
        String chequeStatus = validCheque
                ? "Valid (Payee='" + cheque.getPayeeName() + "', Amount=" + cheque.getAmount() + ", Date=" + cheque.getIssueDate() + ")"
                : "Invalid / Incomplete";

        StringBuilder coords = new StringBuilder();
        if (templateLoaded) {
            coords.append("Payee=").append(formatPos(layout.get(com.chequeprint.model.LayoutField.PAYEE)))
                  .append(", Amount=").append(formatPos(layout.get(com.chequeprint.model.LayoutField.AMOUNT_NUMBER)))
                  .append(", Date=").append(formatPos(layout.get(com.chequeprint.model.LayoutField.DATE)))
                  .append(", Signature=").append(formatPos(layout.get(com.chequeprint.model.LayoutField.SIGNATURE)));
        } else {
            coords.append("N/A (No template)");
        }

        System.out.println("==========================================================");
        System.out.println("[PRE-PRINT DIAGNOSTICS LOG]");
        System.out.println("• Selected Printer Name : " + printerName);
        System.out.println("• Template Loaded       : " + templateInfo);
        System.out.println("• Cheque Data Status    : " + chequeStatus);
        System.out.println("• Coordinates Applied   : " + coords);
        System.out.println("==========================================================");
    }

    private static String formatPos(com.chequeprint.model.FieldPosition pos) {
        if (pos == null) return "(x:0, y:0)";
        return String.format("(x:%.2f, y:%.2f)", pos.getXRatio(), pos.getYRatio());
    }

    // STEP 1: Load template
    public static BankTemplateLayout step1LoadTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        System.out.println("[PrintPipeline] Step 1: Loading template...");
        BankTemplateLayout resolved = layout;
        if (resolved == null) {
            resolved = AppState.getInstance().getSelectedTemplate();
        }
        if (resolved == null) {
            throw new IllegalStateException("[Pipeline Fail at Step 1] No cheque template layout loaded. Please select a bank layout before printing.");
        }
        System.out.println("[PrintPipeline] Step 1 SUCCESS: Loaded layout (" + resolved.getWidthInches() + "x" + resolved.getHeightInches() + " in)");
        return resolved;
    }

    // STEP 2: Load cheque data
    public static Cheque step2LoadChequeData(Cheque cheque) {
        System.out.println("[PrintPipeline] Step 2: Loading & validating cheque data...");
        if (cheque == null) {
            throw new IllegalStateException("[Pipeline Fail at Step 2] Invalid cheque data: Cheque object is null.");
        }
        if (cheque.getPayeeName() == null || cheque.getPayeeName().trim().isEmpty()) {
            throw new IllegalStateException("[Pipeline Fail at Step 2] Invalid cheque data: Payee name is required.");
        }
        if (cheque.getAmount() == null || cheque.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("[Pipeline Fail at Step 2] Invalid cheque data: Amount must be greater than zero.");
        }
        if (cheque.getIssueDate() == null) {
            throw new IllegalStateException("[Pipeline Fail at Step 2] Invalid cheque data: Issue date is required.");
        }
        System.out.println("[PrintPipeline] Step 2 SUCCESS: Validated cheque for payee='" + cheque.getPayeeName() + "', amount=" + cheque.getAmount());
        return cheque;
    }

    // STEP 3: Merge data into template
    public static BankTemplateLayout step3MergeDataIntoTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        System.out.println("[PrintPipeline] Step 3: Merging cheque data into template coordinates...");
        layout.ensureAllFields();
        System.out.println("[PrintPipeline] Step 3 SUCCESS: Merged cheque payee='" + cheque.getPayeeName() + "' with template fields.");
        return layout;
    }

    // STEP 4: Render final cheque layout
    public static Node step4RenderFinalChequeLayout(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        System.out.println("[PrintPipeline] Step 4: Rendering final cheque layout canvas...");
        Pane canvas = new Pane();
        double pointsWidth = layout.getWidthInches() * 72.0;
        double pointsHeight = layout.getHeightInches() * 72.0;
        canvas.setPrefSize(pointsWidth, pointsHeight);

        ChequePreviewEngine.renderPreview(canvas, cheque, bank, layout);
        System.out.println("[PrintPipeline] Step 4 SUCCESS: Rendered final canvas (" + (int)pointsWidth + "x" + (int)pointsHeight + " pt)");
        return canvas;
    }

    // STEP 5: Send to selected printer
    public static boolean step5SendToSelectedPrinter(Node renderedCanvas, Window ownerWindow) {
        System.out.println("[PrintPipeline] Step 5: Sending layout to selected printer...");
        Printer printer = AppState.getInstance().getSelectedPrinter();
        if (printer == null) {
            throw new IllegalStateException("[Pipeline Fail at Step 5] No printer selected. Please select a valid physical printer in Printer Settings before printing.");
        }
        if (!PrinterUtils.isValidPrinter(printer)) {
            String printerName = printer.getName() != null ? printer.getName() : "Unknown";
            throw new IllegalStateException("[Pipeline Fail at Step 5] Selected printer ('" + printerName + "') is invalid. Fax and virtual printers (PDF, XPS, OneNote) are not supported.");
        }

        boolean printed = FxPrinterService.printNode(renderedCanvas, ownerWindow);
        System.out.println("[PrintPipeline] Step 5 SUCCESS: Print job submitted to printer '" + printer.getName() + "', result=" + printed);
        return printed;
    }
}
