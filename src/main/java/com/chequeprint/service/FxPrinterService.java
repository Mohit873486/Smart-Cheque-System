package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AppState;
import com.chequeprint.util.ChequeRenderEngine;
import com.chequeprint.util.PrinterUtils;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.print.Printer.MarginType;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for JavaFX native printing using PrinterJob.
 * Uses the exact same ChequePreviewEngine and template coordinates as screen preview
 * to ensure 100% output consistency with ZERO preview-print mismatch.
 */
public class FxPrinterService {

    private static final Logger LOGGER = Logger.getLogger(FxPrinterService.class.getName());
    private static final double DEFAULT_CHEQUE_WIDTH_INCHES = 8.0;
    private static final double DEFAULT_CHEQUE_HEIGHT_INCHES = 3.66;
    private static final double POINTS_PER_INCH = 72.0;

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

        double widthInches = layout != null ? layout.getWidthInches() : DEFAULT_CHEQUE_WIDTH_INCHES;
        double heightInches = layout != null ? layout.getHeightInches() : DEFAULT_CHEQUE_HEIGHT_INCHES;

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
        return printCheque(node);
    }

    public static boolean printNode(Node node, Window ownerWindow, Printer selectedPrinter) {
        return printCheque(node, selectedPrinter);
    }

    public static boolean printCheque(Node node) {
        return printCheque(node, new PrinterService().resolveSelectedOrDefaultPrinter().orElse(null));
    }

    public static boolean printCheque(Node node, Printer selectedPrinter) {
        if (node == null) {
            LOGGER.warning("[Print] Cannot print: printable node is null.");
            return false;
        }

        final Printer targetPrinter = selectedPrinter != null
                ? selectedPrinter
                : new PrinterService().resolveSelectedOrDefaultPrinter().orElse(null);

        if (targetPrinter == null || !PrinterUtils.isValidPrinter(targetPrinter)) {
            LOGGER.warning("[Print] Cannot print: no selected or default printer is available.");
            PrinterErrorHandler.logFailure(
                    PrinterErrorHandler.FailureType.PRINTER_NOT_FOUND,
                    targetPrinter != null ? targetPrinter.getName() : "<unknown>",
                    "print",
                    null);
            return false;
        }

        final String printerName = targetPrinter.getName();
        PrinterJob job = PrinterJob.createPrinterJob(targetPrinter);
        if (job == null) {
            LOGGER.warning("[Print] Failed to create PrinterJob for selected printer: " + printerName);
            return false;
        }

        LOGGER.info(() -> "[Print] Created job for selected printer '" + printerName
                + "' with initial status: " + job.getJobStatus());

        Scale printScale = null;
        try {
            PageLayout pageLayout = createChequePageLayout(targetPrinter, node);
            printScale = fitNodeInsidePrintableArea(node, pageLayout);

            LOGGER.info(() -> "[Print] Using page layout for '" + printerName
                    + "': paper=" + pageLayout.getPaper().getName()
                    + ", printable=" + pageLayout.getPrintableWidth() + "x" + pageLayout.getPrintableHeight()
                    + ", margins L/R/T/B=" + pageLayout.getLeftMargin() + "/"
                    + pageLayout.getRightMargin() + "/" + pageLayout.getTopMargin() + "/"
                    + pageLayout.getBottomMargin());

            boolean pageQueued = job.printPage(pageLayout, node);
            LOGGER.info(() -> "[Print] printPage result for '" + printerName
                    + "': " + pageQueued + ", status: " + job.getJobStatus());

            if (!pageQueued) {
                PrinterErrorHandler.logFailure(
                        PrinterErrorHandler.FailureType.JOB_FAILED,
                        printerName,
                        "print",
                        null);
                LOGGER.warning("[Print] Print page was rejected or cancelled for printer: " + printerName);
                return false;
            }

            boolean ended = job.endJob();
            LOGGER.info(() -> "[Print] endJob result for '" + printerName
                    + "': " + ended + ", final status: " + job.getJobStatus());

            if (!ended) {
                PrinterErrorHandler.logFailure(
                        PrinterErrorHandler.FailureType.JOB_FAILED,
                        printerName,
                        "print",
                        null);
                LOGGER.warning("[Print] PrinterJob failed while ending job for printer: " + printerName);
            }
            return ended;
        } catch (RuntimeException ex) {
            PrinterErrorHandler.logFailure(
                    PrinterErrorHandler.classify(targetPrinter, ex),
                    printerName,
                    "print",
                    ex);
            LOGGER.log(Level.SEVERE, "[Print] Failed on selected printer '" + printerName
                    + "'. Status: " + job.getJobStatus(), ex);
            return false;
        } finally {
            if (printScale != null) {
                node.getTransforms().remove(printScale);
            }
        }
    }

    private static PageLayout createChequePageLayout(Printer printer, Node node) {
        double widthPoints = resolveNodeWidth(node);
        double heightPoints = resolveNodeHeight(node);

        Paper chequePaper = Paper.A4;

        PageOrientation orientation = PageOrientation.PORTRAIT;

        PageLayout zeroMarginLayout = printer.createPageLayout(
                chequePaper,
                orientation,
                0,
                0,
                0,
                0);

        if (fitsInsidePrintableArea(node, zeroMarginLayout)) {
            return zeroMarginLayout;
        }

        LOGGER.warning("[Print] Printer driver did not accept a full zero-margin cheque layout; using hardware-minimum margins.");
        return printer.createPageLayout(chequePaper, orientation, MarginType.HARDWARE_MINIMUM);
    }

    private static Scale fitNodeInsidePrintableArea(Node node, PageLayout pageLayout) {
        double nodeWidth = resolveNodeWidth(node);
        double nodeHeight = resolveNodeHeight(node);
        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        if (nodeWidth <= 0 || nodeHeight <= 0 || printableWidth <= 0 || printableHeight <= 0) {
            return null;
        }

        double scaleFactor = Math.min(printableWidth / nodeWidth, printableHeight / nodeHeight);
        if (scaleFactor > 0 && scaleFactor < 1.0) {
            Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0);
            node.getTransforms().add(scale);
            LOGGER.info(() -> "[Print] Scaled cheque node to fit printable area: " + scaleFactor);
            return scale;
        }
        return null;
    }

    private static boolean fitsInsidePrintableArea(Node node, PageLayout pageLayout) {
        return resolveNodeWidth(node) <= pageLayout.getPrintableWidth()
                && resolveNodeHeight(node) <= pageLayout.getPrintableHeight();
    }

    private static double resolveNodeWidth(Node node) {
        double width = node.getBoundsInLocal().getWidth();
        if (width <= 0) {
            width = node.prefWidth(-1);
        }
        if (width <= 0) {
            width = DEFAULT_CHEQUE_WIDTH_INCHES * POINTS_PER_INCH;
        }
        return width;
    }

    private static double resolveNodeHeight(Node node) {
        double height = node.getBoundsInLocal().getHeight();
        if (height <= 0) {
            height = node.prefHeight(-1);
        }
        if (height <= 0) {
            height = DEFAULT_CHEQUE_HEIGHT_INCHES * POINTS_PER_INCH;
        }
        return height;
    }
}
