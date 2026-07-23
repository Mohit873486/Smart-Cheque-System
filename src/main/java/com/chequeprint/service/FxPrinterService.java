package com.chequeprint.service;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

/**
 * Service class for JavaFX native printing using PrinterJob.
 * Opens native printer selection dialog and prints any JavaFX Node (cheque preview layout).
 */
public class FxPrinterService {

    /**
     * Shows print dialog to let user select printer, and prints the cheque preview Node.
     *
     * @param node The JavaFX Node (cheque layout/preview container) to print.
     * @param ownerWindow The owner window for displaying the print dialog.
     * @return true if successfully printed, false if cancelled or failed.
     */
    public static boolean printNode(Node node, Window ownerWindow) {
        if (node == null) {
            System.err.println("Cannot print: Node is null.");
            return false;
        }

        // 1. Create PrinterJob
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            System.err.println("No printer services available or failed to create PrinterJob.");
            return false;
        }

        // 2. Show native Print Dialog allowing user to select printer & options
        boolean proceed = job.showPrintDialog(ownerWindow);
        if (!proceed) {
            job.endJob();
            return false; // User cancelled print dialog
        }

        // 3. Obtain selected printer and setup page layout
        Printer selectedPrinter = job.getPrinter();
        PageLayout pageLayout = selectedPrinter.createPageLayout(
                Paper.A4,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM
        );

        // 4. Calculate scaling factor so the cheque layout node fits printable bounds cleanly
        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        double nodeWidth = node.getBoundsInParent().getWidth();
        double nodeHeight = node.getBoundsInParent().getHeight();

        if (nodeWidth <= 0 || nodeHeight <= 0) {
            nodeWidth = 440;
            nodeHeight = 260;
        }

        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        double scaleFactor = Math.min(scaleX, scaleY);

        if (scaleFactor > 1.0) {
            scaleFactor = 1.0; // Keep natural dimensions if node is smaller than printable area
        }

        Scale scaleTransform = new Scale(scaleFactor, scaleFactor);
        node.getTransforms().add(scaleTransform);

        try {
            // 5. Print Node to selected printer
            boolean success = job.printPage(pageLayout, node);
            if (success) {
                job.endJob();
                return true;
            } else {
                job.endJob();
                return false;
            }
        } catch (Exception e) {
            job.endJob();
            System.err.println("Print error: " + e.getMessage());
            return false;
        } finally {
            node.getTransforms().remove(scaleTransform);
        }
    }
}
