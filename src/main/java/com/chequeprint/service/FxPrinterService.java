package com.chequeprint.service;

import com.chequeprint.util.NumberToWordsConverter;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for JavaFX native printing using PrinterJob.
 * Renders cheque preview canvas, applies user data onto exact template positions,
 * opens native printer selection dialog, and prints to the selected printer.
 */
public class FxPrinterService {

    /**
     * Prints cheque using bank template layout specifications and user cheque data.
     *
     * Steps:
     * 1. Render preview Pane
     * 2. Apply user data (payee name, amount, rupee words, date)
     * 3. Use PrinterJob (show print dialog)
     * 4. Print exact positioned content
     *
     * @param payeeName Payee Name string
     * @param amount Numeric Amount
     * @param issueDate Issue Date
     * @param bankName Bank Name
     * @param templateFields Field positions list (name, amount, date, signature)
     * @param ownerWindow Owner Stage/Window for Print Dialog
     * @return true if successfully printed, false if cancelled or failed.
     */
    public static boolean printChequeUsingTemplate(
            String payeeName,
            BigDecimal amount,
            LocalDate issueDate,
            String bankName,
            List<Map<String, Object>> templateFields,
            Window ownerWindow) {

        // Step 1: Render preview Pane
        Pane previewPane = new Pane();
        previewPane.setPrefSize(720, 300);
        previewPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #333333; -fx-border-width: 1px;");

        // Format user data
        String formattedAmount = amount != null ? String.format("%,.2f", amount) : "0.00";
        String amountInWords = amount != null ? NumberToWordsConverter.convert(amount) : "";
        String formattedDate = issueDate != null ? issueDate.format(DateTimeFormatter.ofPattern("dd / MM / yyyy")) : "";

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("name", payeeName != null ? payeeName.trim().toUpperCase() : "");
        dataMap.put("payee", payeeName != null ? payeeName.trim().toUpperCase() : "");
        dataMap.put("amount", "** " + formattedAmount + " **");
        dataMap.put("amount_words", amountInWords);
        dataMap.put("date", formattedDate);

        // Step 2: Apply user data & exact positions on Pane
        if (templateFields != null && !templateFields.isEmpty()) {
            for (Map<String, Object> fieldSpec : templateFields) {
                String fieldName = (String) fieldSpec.get("fieldName");
                Object xObj = fieldSpec.get("xPosition");
                Object yObj = fieldSpec.get("yPosition");
                Object fontFamObj = fieldSpec.get("fontFamily");
                Object fontSizeObj = fieldSpec.get("fontSize");

                if (fieldName != null && xObj instanceof Number && yObj instanceof Number) {
                    double x = ((Number) xObj).doubleValue();
                    double y = ((Number) yObj).doubleValue();
                    String family = fontFamObj instanceof String ? (String) fontFamObj : "Arial";
                    int size = fontSizeObj instanceof Number ? ((Number) fontSizeObj).intValue() : 12;

                    String valueText = dataMap.getOrDefault(fieldName.toLowerCase(), "");
                    if (!valueText.isBlank()) {
                        Label fieldLabel = new Label(valueText);
                        fieldLabel.setFont(Font.font(family, size));
                        fieldLabel.setStyle("-fx-font-family: '" + family + "'; -fx-font-size: " + size + "px; -fx-text-fill: #000000;");
                        fieldLabel.setLayoutX(x);
                        fieldLabel.setLayoutY(y);
                        previewPane.getChildren().add(fieldLabel);
                    }
                }
            }
        } else {
            // Default positioning fallback
            Label dateLabel = new Label(formattedDate);
            dateLabel.setLayoutX(540); dateLabel.setLayoutY(30);
            dateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            Label nameLabel = new Label(payeeName != null ? payeeName.toUpperCase() : "");
            nameLabel.setLayoutX(100); nameLabel.setLayoutY(80);
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            Label wordsLabel = new Label(amountInWords);
            wordsLabel.setLayoutX(120); wordsLabel.setLayoutY(120);
            wordsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;");

            Label amtLabel = new Label("** " + formattedAmount + " **");
            amtLabel.setLayoutX(530); amtLabel.setLayoutY(130);
            amtLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            previewPane.getChildren().addAll(dateLabel, nameLabel, wordsLabel, amtLabel);
        }

        // Steps 3 & 4: Use PrinterJob & Print exact positioned content
        return printNode(previewPane, ownerWindow);
    }

    /**
     * Shows print dialog to let user select printer, and prints any JavaFX Node.
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
            nodeWidth = 720;
            nodeHeight = 300;
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
            // 5. Print exact positioned content to selected printer
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
