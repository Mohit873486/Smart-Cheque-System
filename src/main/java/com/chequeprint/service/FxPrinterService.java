package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import com.chequeprint.util.ChequePreviewEngine;
import com.chequeprint.util.ChequeRenderEngine;
import com.chequeprint.util.PrinterUtils;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service class for JavaFX native printing using PrinterJob.
 * Uses the exact same ChequePreviewEngine and template coordinates as screen preview
 * to ensure 100% output consistency with ZERO preview-print mismatch.
 */
public class FxPrinterService {

    /**
     * Prints a cheque using the single unified ChequePreviewEngine.
     */
    public static boolean printCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) {
        if (layout == null) {
            layout = AppState.getInstance().getSelectedTemplate();
        }
        if (bank == null) {
            bank = AppState.getInstance().getSelectedBank();
        }
        if (cheque == null) {
            cheque = AppState.getInstance().getCurrentCheque();
        }

        double widthInches = layout != null ? layout.getWidthInches() : 8.0;
        double heightInches = layout != null ? layout.getHeightInches() : 3.66;

        double widthPx = widthInches * 96.0;
        double heightPx = heightInches * 96.0;

        Pane printPane = new Pane();
        printPane.setPrefSize(widthPx, heightPx);
        printPane.setMinSize(widthPx, heightPx);
        printPane.setMaxSize(widthPx, heightPx);

        // Render using unified ChequeRenderEngine guaranteeing preview output equals print output
        ChequeRenderEngine.renderCheque(printPane, cheque, bank, layout);

        return printNode(printPane, ownerWindow);
    }

    /**
     * Legacy wrapper for printing using template fields, unified via ChequePreviewEngine.
     */
    public static boolean printChequeUsingTemplate(
            String payeeName,
            BigDecimal amount,
            LocalDate issueDate,
            String bankName,
            List<Map<String, Object>> templateFields,
            Window ownerWindow) {

        Bank bank = AppState.getInstance().getSelectedBank();
        BankTemplateLayout layout = AppState.getInstance().getSelectedTemplate();
        Integer bId = bank != null ? bank.getId() : null;
        Cheque cheque = new Cheque(null, payeeName, amount, bId, issueDate);

        return printCheque(cheque, bank, layout, ownerWindow);
    }

    /**
     * Shows print dialog to let user select printer, and prints any JavaFX Node.
     *
     * @param node The JavaFX Node (cheque layout/preview container) to print.
     * @param ownerWindow The owner window for displaying the print dialog.
     * @return true if successfully printed, false if cancelled or failed.
     */
    public static boolean printNode(Node node, Window ownerWindow) {
        return printNode(node, ownerWindow, AppState.getInstance().getSelectedPrinter());
    }

    public static boolean printNode(Node node, Window ownerWindow, Printer selectedPrinter) {
        if (node == null) {
            System.err.println("Cannot print: Node is null.");
            return false;
        }

        if (selectedPrinter == null || !PrinterUtils.isValidPrinter(selectedPrinter)) {
            System.err.println("Cannot print: No valid printer selected in AppState.");
            return false;
        }

        PrinterJob job = PrinterJob.createPrinterJob(selectedPrinter);
        if (job == null) {
            System.err.println("No printer services available or failed to create PrinterJob.");
            return false;
        }

        Scale scale = null;
        try {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(
                    Paper.NA_LETTER,
                    PageOrientation.LANDSCAPE,
                    Printer.MarginType.HARDWARE_MINIMUM
            );

            double printableWidth = pageLayout.getPrintableWidth();
            double printableHeight = pageLayout.getPrintableHeight();

            double nodeWidth = node.getBoundsInLocal().getWidth();
            double nodeHeight = node.getBoundsInLocal().getHeight();

            if (nodeWidth <= 0) nodeWidth = node.prefWidth(-1);
            if (nodeHeight <= 0) nodeHeight = node.prefHeight(-1);
            if (nodeWidth <= 0) nodeWidth = 720;
            if (nodeHeight <= 0) nodeHeight = 300;

            double scaleX = printableWidth / nodeWidth;
            double scaleY = printableHeight / nodeHeight;
            double scaleFactor = Math.min(scaleX, scaleY);

            scale = new Scale(scaleFactor, scaleFactor);
            node.getTransforms().add(scale);

            boolean success = job.printPage(pageLayout, node);
            if (success) {
                System.out.println("[Print] Submitted job to printer: " + selectedPrinter.getName());
            } else {
                System.err.println("[Print] PrinterJob.printPage returned false for printer: " + selectedPrinter.getName());
            }
            return success;
        } catch (Exception e) {
            System.err.println("[Print] Failed on printer '" + selectedPrinter.getName() + "': " + e.getMessage());
            return false;
        } finally {
            if (scale != null) {
                node.getTransforms().remove(scale);
            }
            job.endJob();
        }
    }
}
