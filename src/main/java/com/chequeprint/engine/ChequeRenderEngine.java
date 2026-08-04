package com.chequeprint.engine;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.util.NumberToWordsConverter;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Unified Cheque Rendering Engine for the entire application.
 * Responsibilities:
 * - Single source of truth for rendering cheques using BankTemplateLayout coordinates.
 * - Used for BOTH screen preview and physical printing.
 * - Guarantees: Preview Output EQUALS Print Output.
 *
 * <p><b>Performance notes:</b>
 * Style strings, alignments, and wrapText flags are pre-computed once in static
 * initializers and applied only when the label cache is first built for a given
 * target pane. Subsequent calls only update text content and x/y positions,
 * avoiding redundant JavaFX CSS re-passes on every render call.
 */
public final class ChequeRenderEngine {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String LABEL_CACHE_KEY = "LABEL_CACHE";

    // ── Performance optimisation: static style / layout data ─────────────────
    // These maps are built once at class-load time.  No String objects are
    // allocated on the hot renderCheque() path — the same interned constant
    // references are reused for every call.
    private static final Map<LayoutField, String>  FIELD_STYLES;
    private static final Map<LayoutField, Pos>     FIELD_ALIGNMENTS;
    private static final Map<LayoutField, Boolean> FIELD_WRAP;

    static {
        FIELD_STYLES = new EnumMap<>(LayoutField.class);
        FIELD_STYLES.put(LayoutField.BANK_LOGO,
                "-fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        FIELD_STYLES.put(LayoutField.PAYEE,
                "-fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        FIELD_STYLES.put(LayoutField.DATE,
                "-fx-font-family: 'Courier New', monospace;"
                + " -fx-font-weight: bold; -fx-font-size: 13px;"
                + " -fx-text-fill: #0f172a; -fx-letter-spacing: 3px;");
        FIELD_STYLES.put(LayoutField.AMOUNT_WORDS,
                "-fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
        FIELD_STYLES.put(LayoutField.AMOUNT_NUMBER,
                "-fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;"
                + " -fx-background-color: rgba(255, 255, 255, 0.7);"
                + " -fx-border-color: #94a3b8; -fx-border-width: 1px;"
                + " -fx-border-radius: 4px; -fx-background-radius: 4px;"
                + " -fx-padding: 2 6 2 6;");
        FIELD_STYLES.put(LayoutField.SIGNATURE,
                "-fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: #475569;");
        FIELD_STYLES.put(LayoutField.MICR,
                "-fx-font-family: 'Courier New', monospace;"
                + " -fx-font-size: 11px; -fx-text-fill: #334155;");

        FIELD_ALIGNMENTS = new EnumMap<>(LayoutField.class);
        FIELD_ALIGNMENTS.put(LayoutField.BANK_LOGO,     Pos.CENTER_LEFT);
        FIELD_ALIGNMENTS.put(LayoutField.PAYEE,         Pos.CENTER_LEFT);
        FIELD_ALIGNMENTS.put(LayoutField.DATE,          Pos.CENTER_LEFT);
        FIELD_ALIGNMENTS.put(LayoutField.AMOUNT_WORDS,  Pos.TOP_LEFT);
        FIELD_ALIGNMENTS.put(LayoutField.AMOUNT_NUMBER, Pos.CENTER_RIGHT);
        FIELD_ALIGNMENTS.put(LayoutField.SIGNATURE,     Pos.BOTTOM_RIGHT);
        FIELD_ALIGNMENTS.put(LayoutField.MICR,          Pos.CENTER);

        FIELD_WRAP = new EnumMap<>(LayoutField.class);
        for (LayoutField f : LayoutField.values()) {
            FIELD_WRAP.put(f, false);
        }
        FIELD_WRAP.put(LayoutField.AMOUNT_WORDS, true);
    }
    // ─────────────────────────────────────────────────────────────────────────

    private ChequeRenderEngine() {
        // Utility class
    }

    public static void initializePreviewElements(Pane targetPane) {
        if (targetPane == null) {
            return;
        }
        getOrCreateLabelCache(targetPane);
    }

    public static void renderCheque(Pane targetPane, Cheque cheque, Bank bank, BankTemplateLayout layout) {
        if (targetPane == null) {
            return;
        }

        if (layout == null) {
            targetPane.setUserData(null);
            renderEmptyState(targetPane);
            return;
        }

        layout.ensureAllFields();

        double targetW = targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : (targetPane.getWidth() > 0 ? targetPane.getWidth() : 720.0);
        double targetH = targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : (targetPane.getHeight() > 0 ? targetPane.getHeight() : 300.0);

        targetPane.setPrefSize(targetW, targetH);
        targetPane.setMinSize(targetW, targetH);
        targetPane.setMaxSize(targetW, targetH);

        if (targetPane.getClip() == null) {
            javafx.scene.shape.Rectangle clipMask = new javafx.scene.shape.Rectangle(targetW, targetH);
            clipMask.setArcWidth(8);
            clipMask.setArcHeight(8);
            targetPane.setClip(clipMask);
        }

        String bankCode = bank != null && bank.getBankCode() != null ? bank.getBankCode().trim().toUpperCase() : "BANK";
        String customLogo = bank != null ? bank.getLogoPath() : null;

        String bgStyle;
        if (customLogo != null && !customLogo.isBlank() && (customLogo.startsWith("http") || customLogo.endsWith(".png") || customLogo.endsWith(".jpg"))) {
            bgStyle = "-fx-background-image: url('" + customLogo + "'); -fx-background-size: cover; -fx-background-position: center; -fx-border-color: #64748b; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
        } else {
            bgStyle = switch (bankCode) {
                case "SBI" -> "-fx-background-color: linear-gradient(to bottom, #dbeafe, #bae6fd); -fx-border-color: #3b82f6; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "HDFC" -> "-fx-background-color: linear-gradient(to bottom, #e0f2fe, #f0f9ff); -fx-border-color: #0284c7; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "ICICI" -> "-fx-background-color: linear-gradient(to bottom, #ffedd5, #fed7aa); -fx-border-color: #ea580c; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                case "AXIS" -> "-fx-background-color: linear-gradient(to bottom, #fce7f3, #fbcfe8); -fx-border-color: #db2777; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
                default -> "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc); -fx-border-color: #64748b; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";
            };
        }
        targetPane.setStyle(bgStyle);

        Map<LayoutField, Label> labelCache = getOrCreateLabelCache(targetPane);

        String bankNameText = bank != null && bank.getBankName() != null ? bank.getBankName() : "State Bank of India";
        String payeeText = cheque != null && cheque.getPayeeName() != null && !cheque.getPayeeName().isBlank()
                ? cheque.getPayeeName()
                : "PAYEE NAME HERE";

        String dateText;
        if (cheque != null && cheque.getIssueDate() != null) {
            dateText = cheque.getIssueDate().format(DATE_FORMATTER);
        } else {
            dateText = LocalDate.now().format(DATE_FORMATTER);
        }

        String amountNumText;
        if (cheque != null && cheque.getAmount() != null) {
            amountNumText = String.format("** %,.2f /-**", cheque.getAmount());
        } else {
            amountNumText = "** 50,000.00 /-**";
        }

        String amountWordsText;
        if (cheque != null && cheque.getAmount() != null) {
            amountWordsText = NumberToWordsConverter.convert(cheque.getAmount().doubleValue());
        } else {
            amountWordsText = "Fifty Thousand Rupees Only";
        }

        String micrText = "⑈ 123456 ⑈ 400240002 ⑉ 000123 ⑈ 31";

        for (LayoutField field : LayoutField.values()) {
            Label lbl = labelCache.get(field);
            if (lbl == null) continue;

            FieldPosition pos = layout.get(field);
            double posX = pos.getXRatio() * targetW;
            double posY = pos.getYRatio() * targetH;
            double fieldW = pos.getWidthRatio() * targetW;
            double fieldH = pos.getHeightRatio() * targetH;

            lbl.setLayoutX(posX);
            lbl.setLayoutY(posY);
            lbl.setPrefWidth(fieldW);
            lbl.setPrefHeight(fieldH);

            // Only text is updated on every call.
            // Style / alignment / wrapText were fixed once at label-creation time above.
            switch (field) {
                case BANK_LOGO     -> lbl.setText(bankNameText);
                case PAYEE         -> lbl.setText(payeeText);
                case DATE          -> lbl.setText(dateText);
                case AMOUNT_WORDS  -> lbl.setText(amountWordsText);
                case AMOUNT_NUMBER -> lbl.setText(amountNumText);
                case SIGNATURE     -> lbl.setText("Authorized Signatory");
                case MICR          -> lbl.setText(micrText);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<LayoutField, Label> getOrCreateLabelCache(Pane targetPane) {
        Map<LayoutField, Label> labelCache = (Map<LayoutField, Label>) targetPane.getProperties().get(LABEL_CACHE_KEY);
        if (labelCache == null) {
            // Create all field labels once when the bank/template preview is initialized.
            labelCache = new EnumMap<>(LayoutField.class);
            for (LayoutField field : LayoutField.values()) {
                Label lbl = new Label();
                lbl.setMouseTransparent(true);
                lbl.setStyle(FIELD_STYLES.get(field));
                lbl.setAlignment(FIELD_ALIGNMENTS.get(field));
                lbl.setWrapText(Boolean.TRUE.equals(FIELD_WRAP.get(field)));
                labelCache.put(field, lbl);
                targetPane.getChildren().add(lbl);
            }
            targetPane.getProperties().put(LABEL_CACHE_KEY, labelCache);
        } else if (!targetPane.getChildren().containsAll(labelCache.values())) {
            targetPane.getChildren().setAll(labelCache.values());
        }
        return labelCache;
    }

    public static WritableImage renderChequeToImage(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        Pane canvas = new Pane();
        double w = (layout != null ? layout.getWidthInches() : 8.0) * 72.0;
        double h = (layout != null ? layout.getHeightInches() : 3.66) * 72.0;
        canvas.setPrefSize(w, h);
        canvas.setMinSize(w, h);
        canvas.setMaxSize(w, h);
        canvas.resize(w, h);

        renderCheque(canvas, cheque, bank, layout);

        javafx.scene.Scene dummyScene = new javafx.scene.Scene(canvas, w, h);
        canvas.applyCss();
        canvas.layout();

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        return canvas.snapshot(params, null);
    }

    public static void renderLoadingState(Pane targetPane, String message) {
        if (targetPane == null) return;
        targetPane.getChildren().clear();
        targetPane.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 1.5px; -fx-border-radius: 8px;");

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(32, 32);

        Label label = new Label(message != null ? message : "Loading layout...");
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        box.getChildren().addAll(pi, label);
        box.setPrefSize(targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : 720, targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : 300);
        targetPane.getChildren().add(box);
    }

    public static void renderEmptyState(Pane targetPane) {
        if (targetPane == null) return;
        targetPane.getChildren().clear();
        targetPane.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 1.5px; -fx-border-style: dashed; -fx-border-radius: 8px;");

        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);

        Label title = new Label("No Bank Template Selected");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #475569;");

        Label subtitle = new Label("Please select a bank account to render cheque template preview.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        box.getChildren().addAll(title, subtitle);
        box.setPrefSize(targetPane.getPrefWidth() > 0 ? targetPane.getPrefWidth() : 720, targetPane.getPrefHeight() > 0 ? targetPane.getPrefHeight() : 300);
        targetPane.getChildren().add(box);
    }
}
