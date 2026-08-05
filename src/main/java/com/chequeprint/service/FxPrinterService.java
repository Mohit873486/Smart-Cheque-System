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
        return printCheque(node, new PrintService().resolveSelectedOrDefaultPrinter().orElse(Printer.getDefaultPrinter()));
    }

    public static boolean printCheque(Node node, Printer selectedPrinter) {
        // 1. Null checks
        if (node == null) {
            System.out.println("Print Failed");
            LOGGER.warning("[Print] Printable node is null.");
            return false;
        }

        final Printer targetPrinter = selectedPrinter != null
                ? selectedPrinter
                : new PrintService().resolveSelectedOrDefaultPrinter().orElse(Printer.getDefaultPrinter());

        if (targetPrinter == null || !PrinterUtils.isValidPrinter(targetPrinter)) {
            System.out.println("Print Failed");
            LOGGER.warning("[Print] No selected or default printer available.");
            PrinterErrorHandler.logFailure(
                    PrinterErrorHandler.FailureType.PRINTER_NOT_FOUND,
                    targetPrinter != null ? targetPrinter.getName() : "<unknown>",
                    "print",
                    null);
            return false;
        }

        // 2. Create PrinterJob for targetPrinter
        final String printerName = targetPrinter.getName();
        PrinterJob job = PrinterJob.createPrinterJob(targetPrinter);
        if (job == null) {
            System.out.println("Print Failed");
            LOGGER.warning("[Print] Failed to create PrinterJob for printer: " + printerName);
            return false;
        }

        // 3. Log "Print Started"
        System.out.println("Print Started");
        LOGGER.info(() -> "[Print] Created PrinterJob for printer '" + printerName + "'");

        Scale printScale = null;
        try {
            PageLayout pageLayout = createChequePageLayout(targetPrinter, node);
            printScale = fitNodeInsidePrintableArea(node, pageLayout);

            // 4. Call printPage
            boolean pageQueued = job.printPage(pageLayout, node);

            // 5. Call job.endJob() ONLY if printPage succeeded
            if (pageQueued) {
                boolean ended = job.endJob();
                if (ended) {
                    System.out.println("Print Success");
                    LOGGER.info(() -> "[Print] Print job successfully ended for printer: " + printerName);
                    return true;
                } else {
                    PrinterErrorHandler.logFailure(
                            PrinterErrorHandler.FailureType.JOB_FAILED,
                            printerName,
                            "print",
                            null);
                    LOGGER.warning("[Print] PrinterJob failed while ending job for printer: " + printerName);
                }
            } else {
                PrinterErrorHandler.logFailure(
                        PrinterErrorHandler.FailureType.JOB_FAILED,
                        printerName,
                        "print",
                        null);
                LOGGER.warning("[Print] Print page was rejected for printer: " + printerName);
            }

            System.out.println("Print Failed");
            return false;

        } catch (Exception ex) {
            System.out.println("Print Failed");
            PrinterErrorHandler.logFailure(
                    PrinterErrorHandler.classify(targetPrinter, ex),
                    printerName,
                    "print",
                    ex);
            LOGGER.log(Level.SEVERE, "[Print] Failed on printer '" + printerName + "'", ex);
            return false;
        } finally {
            if (printScale != null) {
                node.getTransforms().remove(printScale);
            }
        }
    }

    public static PageLayout createChequePageLayout(Printer printer, Node node) {
        if (printer == null) {
            printer = Printer.getDefaultPrinter();
        }

        double widthPoints = resolveNodeWidth(node);
        double heightPoints = resolveNodeHeight(node);

        // Cheques are wider than tall -> LANDSCAPE if width > height
        PageOrientation orientation = widthPoints > heightPoints
                ? PageOrientation.LANDSCAPE
                : PageOrientation.PORTRAIT;

        Paper chequePaper = Paper.A4;

        // Create PageLayout with 0 margins (HARDWARE_MINIMUM fallback if printer driver rejects 0)
        try {
            PageLayout zeroMarginLayout = printer.createPageLayout(
                    chequePaper,
                    orientation,
                    0, 0, 0, 0
            );
            if (fitsInsidePrintableArea(node, zeroMarginLayout)) {
                return zeroMarginLayout;
            }
        } catch (Exception ignored) {}

        return printer.createPageLayout(chequePaper, orientation, MarginType.HARDWARE_MINIMUM);
    }

    public static Scale fitNodeInsidePrintableArea(Node node, PageLayout pageLayout) {
        if (node == null || pageLayout == null) {
            return null;
        }

        double nodeWidth = resolveNodeWidth(node);
        double nodeHeight = resolveNodeHeight(node);
        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        if (nodeWidth <= 0 || nodeHeight <= 0 || printableWidth <= 0 || printableHeight <= 0) {
            return null;
        }

        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        double scaleFactor = Math.min(1.0, Math.min(scaleX, scaleY));

        if (scaleFactor < 1.0) {
            Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0);
            node.getTransforms().add(scale);
            LOGGER.info(() -> "[Print] Applied scale factor to fit cheque inside printable bounds: " + scaleFactor);
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
