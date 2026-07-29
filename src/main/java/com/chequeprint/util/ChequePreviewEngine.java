package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Single Dedicated Cheque Preview Engine for the entire application.
 * Features professional UX rendering with loading indicators, error feedback,
 * empty states, and dynamic 1-to-1 template ratio coordinate mapping.
 */
public final class ChequePreviewEngine {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ChequePreviewEngine() {
        // Utility class
    }

    /**
     * Renders a complete cheque preview onto the specified target Pane.
     *
     * @param targetPane JavaFX container pane to render preview inside
     * @param cheque     Active cheque data (can be null for placeholder defaults)
     * @param bank       Active bank details
     * @param layout     Template layout containing field coordinates
     */
    public static void renderPreview(Pane targetPane, Cheque cheque, Bank bank, BankTemplateLayout layout) {
        if (targetPane == null) {
            return;
        }

        // Empty state guard: Display professional empty state if template is null
        if (layout == null) {
            targetPane.setUserData(null);
            renderEmptyState(targetPane);
            return;
        }

        // State signature deduplication guard: Skip redundant re-renders for smooth UI
        String signature = buildStateSignature(cheque, bank, layout);
        if (signature.equals(targetPane.getUserData())) {
            return;
        }
        targetPane.setUserData(signature);

        // Ensure all fields have template coordinates
        layout.ensureAllFields();

        double targetW = targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : (targetPane.getWidth() > 0 ? targetPane.getWidth() : 720.0);
        double targetH = targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : (targetPane.getHeight() > 0 ? targetPane.getHeight() : 300.0);

        double paneW = targetW;
        double paneH = targetH;

        targetPane.setPrefSize(paneW, paneH);
        targetPane.setMinSize(paneW, paneH);
        targetPane.setMaxSize(paneW, paneH);

        // Strict clipping mask so text elements NEVER overflow outside the preview box
        if (targetPane.getClip() == null) {
            javafx.scene.shape.Rectangle clipMask = new javafx.scene.shape.Rectangle(paneW, paneH);
            clipMask.setArcWidth(8);
            clipMask.setArcHeight(8);
            targetPane.setClip(clipMask);
        }

        String bankCode = bank != null && bank.getBankCode() != null ? bank.getBankCode().trim().toUpperCase() : "BANK";
        String customLogo = bank != null ? bank.getLogoPath() : null;

        // 1. Background Cheque Image & Stylized Border
        String patternOverlay = ", repeating-linear-gradient(to bottom, transparent, transparent 11px, rgba(203, 213, 225, 0.25) 11px, rgba(203, 213, 225, 0.25) 12px);";
        String bgStyle;
        if (customLogo != null && !customLogo.isBlank() && (customLogo.startsWith("http") || customLogo.endsWith(".png") || customLogo.endsWith(".jpg"))) {
            bgStyle = "-fx-background-image: url('" + customLogo + "'); -fx-background-size: cover; -fx-background-position: center; -fx-border-color: #64748b; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
        } else {
            bgStyle = switch (bankCode) {
                case "SBI" -> "-fx-background-color: linear-gradient(to bottom, #dbeafe, #bae6fd)" + patternOverlay + " -fx-border-color: #3b82f6; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "HDFC" -> "-fx-background-color: linear-gradient(to bottom, #e0f2fe, #f0f9ff)" + patternOverlay + " -fx-border-color: #0284c7; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "ICICI" -> "-fx-background-color: linear-gradient(to bottom, #ffedd5, #fed7aa)" + patternOverlay + " -fx-border-color: #ea580c; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "AXIS" -> "-fx-background-color: linear-gradient(to bottom, #fce7f3, #fbcfe8)" + patternOverlay + " -fx-border-color: #db2777; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                default -> "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc)" + patternOverlay + " -fx-border-color: #64748b; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
            };
        }
        targetPane.setStyle(bgStyle);

        // 2. Minimal UI Nodes Initialization (Only 7 Label text nodes, ZERO extra Line/Shape nodes)
        @SuppressWarnings("unchecked")
        Map<LayoutField, Label> labelCache = (Map<LayoutField, Label>) targetPane.getProperties().get("LABEL_CACHE");
        if (labelCache == null) {
            targetPane.getChildren().clear();

            labelCache = new java.util.EnumMap<>(LayoutField.class);
            for (LayoutField field : LayoutField.values()) {
                Label lbl = new Label();
                lbl.setMouseTransparent(true);
                labelCache.put(field, lbl);
                targetPane.getChildren().add(lbl);
            }
            targetPane.getProperties().put("LABEL_CACHE", labelCache);
        }

        // 3. Dynamic values to render
        String bankNameText = bank != null && bank.getBankName() != null ? bank.getBankName() : "State Bank of India";
        String payeeText = cheque != null && cheque.getPayeeName() != null && !cheque.getPayeeName().isBlank()
                ? cheque.getPayeeName()
                : "PAYEE NAME HERE";

        String amountText = cheque != null && cheque.getAmount() != null
                ? "₹ " + String.format("%,.2f", cheque.getAmount()) + "/-"
                : "₹ 10,500.00/-";

        String wordsText = cheque != null && cheque.getAmount() != null
                ? NumberToWordsConverter.convert(cheque.getAmount())
                : "Rupees Ten Thousand Five Hundred Only";

        String dateText = cheque != null && cheque.getIssueDate() != null
                ? cheque.getIssueDate().format(DATE_FORMATTER)
                : LocalDate.now().format(DATE_FORMATTER);

        String chequeNoText = cheque != null && cheque.getChequeNo() != null ? cheque.getChequeNo() : "123456";

        // 4. Update cached nodes IN-PLACE with zero node recreation
        double scale = Math.max(0.4, paneW / 720.0);
        for (LayoutField field : LayoutField.values()) {
            FieldPosition pos = layout.get(field);
            Label label = labelCache.get(field);
            if (label == null) continue;

            if (pos == null) {
                label.setVisible(false);
                continue;
            }

            label.setVisible(true);
            double x = pos.getXRatio() * paneW;
            double y = pos.getYRatio() * paneH;
            double w = Math.max(30.0, pos.getWidthRatio() * paneW);
            double h = Math.max(16.0, pos.getHeightRatio() * paneH);

            label.setLayoutX(x);
            label.setLayoutY(y);
            label.setPrefSize(w, h);

            switch (field) {
                case BANK_LOGO -> {
                    label.setText(bankNameText);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(9, (int)(13 * scale)) + "px; -fx-text-fill: #1e3a8a; -fx-background-color: transparent;");
                }
                case DATE -> {
                    label.setText(dateText);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(9, (int)(12 * scale)) + "px; -fx-text-fill: #0f172a; -fx-background-color: transparent;");
                }
                case PAYEE -> {
                    label.setText(payeeText);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(9, (int)(12 * scale)) + "px; -fx-text-fill: #0f172a; -fx-background-color: transparent;");
                }
                case AMOUNT_WORDS -> {
                    label.setText(wordsText);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(8, (int)(11 * scale)) + "px; -fx-text-fill: #1e293b; -fx-background-color: transparent;");
                }
                case AMOUNT_NUMBER -> {
                    label.setText(amountText);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(9, (int)(13 * scale)) + "px; -fx-text-fill: #0f172a; -fx-background-color: transparent;");
                }
                case SIGNATURE -> {
                    label.setText("Authorized Signatory");
                    label.setStyle("-fx-font-size: " + Math.max(8, (int)(10 * scale)) + "px; -fx-text-fill: #475569; -fx-background-color: transparent;");
                }
                case MICR -> {
                    label.setText("⑈" + chequeNoText + "⑈ 000000000 ⑈00⑈");
                    label.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: " + Math.max(8, (int)(11 * scale)) + "px; -fx-text-fill: #334155; -fx-background-color: transparent;");
                }
            }
        }
    }

    /**
     * Renders a professional loading spinner indicator inside the preview pane.
     */
    public static void renderLoadingState(Pane targetPane, String message) {
        if (targetPane == null) return;
        targetPane.getChildren().clear();

        double w = targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : 720;
        double h = targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : 300;

        VBox container = new VBox(12);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(w, h);
        container.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);

        Label label = new Label(message != null ? message : "Fetching cheque template layout...");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        container.getChildren().addAll(spinner, label);
        targetPane.getChildren().add(container);
    }

    /**
     * Renders a professional error state container inside the preview pane.
     */
    public static void renderErrorState(Pane targetPane, String errorMessage) {
        if (targetPane == null) return;
        targetPane.getChildren().clear();

        double w = targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : 720;
        double h = targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : 300;

        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(w, h);
        container.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fca5a5; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 26px;");

        Label title = new Label("Template Load Error");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");

        Label detail = new Label(errorMessage != null ? errorMessage : "Failed to load cheque template from server.");
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f1d1d;");

        container.getChildren().addAll(icon, title, detail);
        targetPane.getChildren().add(container);
    }

    /**
     * Renders a modern empty state container inside the preview pane when no template is selected.
     */
    public static void renderEmptyState(Pane targetPane) {
        if (targetPane == null) return;
        targetPane.getChildren().clear();

        double w = targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : 720;
        double h = targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : 300;

        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(w, h);
        container.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-style: dashed; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label icon = new Label("🏛️");
        icon.setStyle("-fx-font-size: 28px;");

        Label title = new Label("No Bank Template Loaded");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Label subtitle = new Label("Select a bank account from the list to preview and edit its cheque template.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        container.getChildren().addAll(icon, title, subtitle);
        targetPane.getChildren().add(container);
    }

    private static String buildStateSignature(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        String cStr = cheque != null ? (cheque.getId() + ":" + cheque.getPayeeName() + ":" + cheque.getAmount() + ":" + cheque.getIssueDate()) : "c_null";
        String bStr = bank != null ? (bank.getId() + ":" + bank.getBankCode()) : "b_null";
        String lStr = layout != null ? layout.getFieldPositions().toString() : "l_null";
        return cStr + "|" + bStr + "|" + lStr;
    }
}
