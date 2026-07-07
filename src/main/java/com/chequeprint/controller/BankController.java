package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.service.BankService;
import com.chequeprint.util.BankTemplatePdfExporter;
import com.chequeprint.util.ChequeSizeCodec;
import com.chequeprint.util.ChequeSizePreset;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.io.File;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankController {

    private static final double PREVIEW_PPI = 90.0;

    @FXML
    private TextField fldBankName;
    @FXML
    private TextField fldBankCode;
    @FXML
    private ComboBox<ChequeSizePreset> cmbChequeSize;
    @FXML
    private CheckBox chkMicr;
    @FXML
    private TextField fldCustomWidth;
    @FXML
    private TextField fldCustomHeight;
    @FXML
    private Button btnSave;

    @FXML
    private TableView<Bank> bankTable;
    @FXML
    private TableColumn<Bank, String> colBankName;
    @FXML
    private TableColumn<Bank, String> colBankCode;
    @FXML
    private TableColumn<Bank, String> colChequeSize;
    @FXML
    private TableColumn<Bank, String> colMicr;

    @FXML
    private Label lblFormTitle;
    @FXML
    private Label lblPreviewSize;
    @FXML
    private AnchorPane previewViewport;
    @FXML
    private Pane chequePreviewPane;
    @FXML
    private ComboBox<LayoutField> cmbAdjustField;
    @FXML
    private TextField fldAdjustLeft;
    @FXML
    private TextField fldAdjustTop;
    @FXML
    private TextField fldAdjustWidth;
    @FXML
    private TextField fldAdjustHeight;

    private final BankService bankService = new BankService();
    private final ObservableList<Bank> data = FXCollections.observableArrayList();

    private final Map<String, BankTemplateLayout> layoutByBankCode = new HashMap<>();
    private final Map<LayoutField, Label> fieldNodes = new EnumMap<>(LayoutField.class);

    private Bank selectedBank;
    private BankTemplateLayout currentLayout;

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        setupPreview();
        setupAdjustmentPanel();
        loadLayouts();
        loadData();
        FxUtils.animateIn(previewViewport, 0);
    }

    private void setupTable() {
        colBankName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBankName()));
        colBankCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBankCode()));
        colChequeSize.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                ChequeSizeCodec.display(c.getValue().getChequeSize())));
        colMicr.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().isMicr() ? "Yes" : "No"));

        bankTable.setItems(data);
        bankTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                populateForm(sel);
            }
        });
    }

    private void setupForm() {
        cmbChequeSize.setItems(FXCollections.observableArrayList(ChequeSizePreset.values()));
        cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        chkMicr.setSelected(true);

        fldCustomWidth.setDisable(true);
        fldCustomHeight.setDisable(true);

        cmbChequeSize.valueProperty().addListener((obs, old, preset) -> {
            boolean custom = preset == ChequeSizePreset.CUSTOM;
            fldCustomWidth.setDisable(!custom);
            fldCustomHeight.setDisable(!custom);
            if (!custom) {
                fldCustomWidth.clear();
                fldCustomHeight.clear();
            }
            refreshLayoutForSizeChange();
        });

        chkMicr.selectedProperty().addListener((obs, o, n) -> refreshPreview());
    }

    private void setupPreview() {
        chequePreviewPane.setStyle("-fx-background-color:white; -fx-border-color:#94a3b8; -fx-border-width:1; -fx-background-radius:10; -fx-border-radius:10;");
        createFieldNode(LayoutField.BANK_LOGO, "BANK", "-fx-background-color:#eff6ff; -fx-border-color:#3b82f6;");
        createFieldNode(LayoutField.DATE, "DATE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.PAYEE, "PAYEE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.AMOUNT_NUMBER, "AMOUNT", "-fx-background-color:#fefce8; -fx-border-color:#ca8a04;");
        createFieldNode(LayoutField.AMOUNT_WORDS, "WORDS", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.SIGNATURE, "SIGN", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.MICR, "MICR", "-fx-background-color:#f1f5f9; -fx-border-color:#334155;");

        previewViewport.widthProperty().addListener((obs, old, v) -> layoutPreviewPane());
        previewViewport.heightProperty().addListener((obs, old, v) -> layoutPreviewPane());
    }

    private void setupAdjustmentPanel() {
        cmbAdjustField.setItems(FXCollections.observableArrayList(LayoutField.values()));
        cmbAdjustField.setValue(LayoutField.PAYEE);
        cmbAdjustField.valueProperty().addListener((obs, old, field) -> loadAdjustmentFields(field));
    }

    private void createFieldNode(LayoutField field, String text, String style) {
        Label label = new Label(text);
        label.setPadding(new Insets(2, 5, 2, 5));
        label.setStyle(style + " -fx-font-size:11px; -fx-font-weight:600;");
        label.setCursor(Cursor.MOVE);
        enableDrag(field, label);
        fieldNodes.put(field, label);
        chequePreviewPane.getChildren().add(label);
    }

    private void enableDrag(LayoutField field, Label node) {
        final Delta delta = new Delta();
        node.setOnMousePressed(e -> {
            delta.x = e.getX();
            delta.y = e.getY();
            e.consume();
        });
        node.setOnMouseDragged(e -> {
            moveFieldNode(field, node, e, delta);
            e.consume();
        });
        node.setOnMouseReleased(e -> {
            persistCurrentLayoutIfPossible();
            e.consume();
        });
    }

    private void moveFieldNode(LayoutField field, Label node, MouseEvent event, Delta delta) {
        if (currentLayout == null) {
            return;
        }

        double nx = node.getLayoutX() + (event.getX() - delta.x);
        double ny = node.getLayoutY() + (event.getY() - delta.y);

        nx = clamp(nx, 0, Math.max(0, chequePreviewPane.getWidth() - node.getWidth()));
        ny = clamp(ny, 0, Math.max(0, chequePreviewPane.getHeight() - node.getHeight()));

        node.setLayoutX(nx);
        node.setLayoutY(ny);

        double xr = chequePreviewPane.getWidth() <= 0 ? 0 : nx / chequePreviewPane.getWidth();
        double yr = chequePreviewPane.getHeight() <= 0 ? 0 : ny / chequePreviewPane.getHeight();
        currentLayout.setFieldPosition(field, xr, yr);
        if (field == cmbAdjustField.getValue()) {
            loadAdjustmentFields(field);
        }
    }

    private void layoutPreviewPane() {
        if (currentLayout == null) {
            return;
        }
        double widthPx = currentLayout.getWidthInches() * PREVIEW_PPI;
        double heightPx = currentLayout.getHeightInches() * PREVIEW_PPI;

        double vw = previewViewport.getWidth();
        double vh = previewViewport.getHeight();
        if (vw <= 0 || vh <= 0 || widthPx <= 0 || heightPx <= 0) {
            return;
        }

        double scale = Math.min((vw - 24) / widthPx, (vh - 24) / heightPx);
        scale = Math.max(0.25, scale);

        chequePreviewPane.setPrefSize(widthPx, heightPx);
        chequePreviewPane.setScaleX(scale);
        chequePreviewPane.setScaleY(scale);

        double px = (vw - widthPx * scale) / 2.0;
        double py = (vh - heightPx * scale) / 2.0;
        AnchorPane.setLeftAnchor(chequePreviewPane, px);
        AnchorPane.setTopAnchor(chequePreviewPane, py);

        refreshPreview();
    }

    private void refreshPreview() {
        if (currentLayout == null) {
            return;
        }
        currentLayout.ensureAllFields();

        for (Map.Entry<LayoutField, Label> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            Label node = entry.getValue();
            FieldPosition pos = currentLayout.get(field);
            double x = pos.getXRatio() * chequePreviewPane.getWidth();
            double y = pos.getYRatio() * chequePreviewPane.getHeight();
            double w = fieldWidthPx(field, pos);
            double h = fieldHeightPx(field, pos);
            node.setPrefSize(w, h);
            node.setMinSize(w, h);
            node.setMaxSize(w, h);
            x = clamp(x, 0, Math.max(0, chequePreviewPane.getWidth() - node.getWidth()));
            y = clamp(y, 0, Math.max(0, chequePreviewPane.getHeight() - node.getHeight()));
            node.setLayoutX(x);
            node.setLayoutY(y);
            node.setVisible(field != LayoutField.MICR || chkMicr.isSelected());
        }

        lblPreviewSize.setText(String.format("Preview Size: %.2f x %.2f inches", currentLayout.getWidthInches(), currentLayout.getHeightInches()));
        loadAdjustmentFields(cmbAdjustField.getValue());
    }

    @FXML
    private void onApplyFieldAdjustment() {
        if (currentLayout == null || cmbAdjustField.getValue() == null) {
            return;
        }

        try {
            LayoutField field = cmbAdjustField.getValue();
            double widthMm = currentLayout.getWidthInches() * 25.4;
            double heightMm = currentLayout.getHeightInches() * 25.4;

            double leftMm = parsePositive(fldAdjustLeft.getText(), "Left");
            double topMm = parsePositive(fldAdjustTop.getText(), "Top");
            double fieldWidthMm = parsePositive(fldAdjustWidth.getText(), "Width");
            double fieldHeightMm = parsePositive(fldAdjustHeight.getText(), "Height");

            fieldWidthMm = Math.min(fieldWidthMm, Math.max(1.0, widthMm - leftMm));
            fieldHeightMm = Math.min(fieldHeightMm, Math.max(1.0, heightMm - topMm));

            currentLayout.setFieldLayout(
                    field,
                    leftMm / widthMm,
                    topMm / heightMm,
                    fieldWidthMm / widthMm,
                    fieldHeightMm / heightMm);
            refreshPreview();
            persistCurrentLayoutIfPossible();
        } catch (IllegalArgumentException ex) {
            showAlert("Adjustment", ex.getMessage(), Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void onResetDefaultLayout() {
        if (currentLayout == null) {
            return;
        }

        currentLayout = new BankTemplateLayout(currentLayout.getWidthInches(), currentLayout.getHeightInches());
        layoutPreviewPane();
        refreshPreview();
        persistCurrentLayoutIfPossible();
    }

    private void loadLayouts() {
        layoutByBankCode.clear();
        layoutByBankCode.putAll(bankService.loadAllLayouts());
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<Bank> list = bankService.getAll();
                Platform.runLater(() -> data.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Load Error", e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "load-banks").start();
    }

    @FXML
    private void onSave() {
        String name = fldBankName.getText().trim();
        String code = fldBankCode.getText().trim().toUpperCase();
        if (name.isEmpty() || code.isEmpty()) {
            showAlert("Validation", "Bank name and bank code are required.", Alert.AlertType.WARNING);
            return;
        }

        ChequeSizePreset preset = cmbChequeSize.getValue();
        BankTemplateLayout formLayout = buildLayoutFromFormSize();
        if (formLayout == null) {
            return;
        }

        String encodedSize = ChequeSizeCodec.encode(preset, formLayout.getWidthInches(), formLayout.getHeightInches());

        Bank bank = selectedBank;
        if (bank == null) {
            bank = new Bank(name, code, encodedSize, chkMicr.isSelected());
        } else {
            bank.setBankName(name);
            bank.setBankCode(code);
            bank.setChequeSize(encodedSize);
            bank.setMicr(chkMicr.isSelected());
        }

        final Bank finalBank = bank;
        final BankTemplateLayout layoutToSave = currentLayout != null ? currentLayout.copy() : formLayout.copy();

        new Thread(() -> {
            try {
                bankService.save(finalBank, layoutToSave, layoutByBankCode);
                Platform.runLater(() -> {
                    clearForm();
                    loadData();
                    showAlert("Success", "Bank template saved successfully.", Alert.AlertType.INFORMATION);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Save Error", e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "save-bank").start();
    }

    @FXML
    private void onDelete() {
        Bank sel = bankTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select", "Please select a bank template to delete.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete bank template for " + sel.getBankName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        bankService.delete(sel, layoutByBankCode);
                        Platform.runLater(() -> {
                            clearForm();
                            loadData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Delete Error", e.getMessage(), Alert.AlertType.ERROR));
                    }
                }, "delete-bank").start();
            }
        });
    }

    @FXML
    private void onExportPdf() {
        if (currentLayout == null) {
            showAlert("Preview", "No template layout available to export.", Alert.AlertType.WARNING);
            return;
        }

        Bank bank = selectedBank != null ? selectedBank : buildDraftBank();
        if (bank.getBankName() == null || bank.getBankName().isBlank() || bank.getBankCode() == null || bank.getBankCode().isBlank()) {
            showAlert("Validation", "Enter bank name and code before exporting PDF.", Alert.AlertType.WARNING);
            return;
        }

        try {
            String home = System.getProperty("user.home");
            String outDir = home + File.separator + "Downloads";
            String pdfPath = BankTemplatePdfExporter.export(bank, currentLayout, outDir);
            showAlert("PDF Exported", "Template PDF saved to:\n" + pdfPath, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Export Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void populateForm(Bank bank) {
        selectedBank = bank;
        lblFormTitle.setText("Edit Bank Template");
        btnSave.setText("Update Template");

        fldBankName.setText(bank.getBankName());
        fldBankCode.setText(bank.getBankCode());
        chkMicr.setSelected(bank.isMicr());

        ChequeSizePreset preset = ChequeSizePreset.fromValue(bank.getChequeSize());
        cmbChequeSize.setValue(preset);

        if (preset == ChequeSizePreset.CUSTOM) {
            BankTemplateLayout sizeLayout = ChequeSizeCodec.decodeLayout(bank.getChequeSize());
            fldCustomWidth.setText(String.format("%.2f", sizeLayout.getWidthInches()));
            fldCustomHeight.setText(String.format("%.2f", sizeLayout.getHeightInches()));
            fldCustomWidth.setDisable(false);
            fldCustomHeight.setDisable(false);
        } else {
            fldCustomWidth.clear();
            fldCustomHeight.clear();
            fldCustomWidth.setDisable(true);
            fldCustomHeight.setDisable(true);
        }

        String code = safeCode(bank.getBankCode());
        BankTemplateLayout savedLayout = layoutByBankCode.get(code);
        currentLayout = savedLayout != null ? savedLayout.copy() : ChequeSizeCodec.decodeLayout(bank.getChequeSize());
        currentLayout.ensureAllFields();

        layoutPreviewPane();
        refreshPreview();
    }

    private void clearForm() {
        selectedBank = null;
        lblFormTitle.setText("Add New Bank Template");
        btnSave.setText("Save Template");
        fldBankName.clear();
        fldBankCode.clear();
        chkMicr.setSelected(true);
        cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        fldCustomWidth.clear();
        fldCustomHeight.clear();

        currentLayout = new BankTemplateLayout(ChequeSizePreset.STANDARD.getWidthInches(), ChequeSizePreset.STANDARD.getHeightInches());
        layoutPreviewPane();
        refreshPreview();
        bankTable.getSelectionModel().clearSelection();
    }

    private void refreshLayoutForSizeChange() {
        BankTemplateLayout sizeLayout = buildLayoutFromFormSize();
        if (sizeLayout == null) {
            return;
        }
        if (currentLayout == null) {
            currentLayout = sizeLayout;
        } else {
            currentLayout.setWidthInches(sizeLayout.getWidthInches());
            currentLayout.setHeightInches(sizeLayout.getHeightInches());
            currentLayout.ensureAllFields();
        }
        layoutPreviewPane();
        refreshPreview();
    }

    private BankTemplateLayout buildLayoutFromFormSize() {
        ChequeSizePreset preset = cmbChequeSize.getValue();
        if (preset == null) {
            preset = ChequeSizePreset.STANDARD;
        }

        if (preset != ChequeSizePreset.CUSTOM) {
            return new BankTemplateLayout(preset.getWidthInches(), preset.getHeightInches());
        }

        try {
            double w = Double.parseDouble(fldCustomWidth.getText().trim());
            double h = Double.parseDouble(fldCustomHeight.getText().trim());
            if (w <= 0 || h <= 0) {
                throw new NumberFormatException("Size must be positive.");
            }
            return new BankTemplateLayout(w, h);
        } catch (NumberFormatException ex) {
            showAlert("Validation", "Enter valid custom width and height (inches).", Alert.AlertType.WARNING);
            return null;
        }
    }

    private Bank buildDraftBank() {
        Bank bank = new Bank();
        bank.setBankName(fldBankName.getText().trim());
        bank.setBankCode(fldBankCode.getText().trim().toUpperCase());
        bank.setMicr(chkMicr.isSelected());
        return bank;
    }

    private void loadAdjustmentFields(LayoutField field) {
        if (currentLayout == null || field == null || fldAdjustLeft == null) {
            return;
        }

        FieldPosition pos = currentLayout.get(field);
        double widthMm = currentLayout.getWidthInches() * 25.4;
        double heightMm = currentLayout.getHeightInches() * 25.4;

        fldAdjustLeft.setText(formatMm(pos.getXRatio() * widthMm));
        fldAdjustTop.setText(formatMm(pos.getYRatio() * heightMm));
        fldAdjustWidth.setText(formatMm(effectiveWidthRatio(field, pos) * widthMm));
        fldAdjustHeight.setText(formatMm(effectiveHeightRatio(field, pos) * heightMm));
    }

    private double fieldWidthPx(LayoutField field, FieldPosition pos) {
        return Math.max(24.0, effectiveWidthRatio(field, pos) * chequePreviewPane.getWidth());
    }

    private double fieldHeightPx(LayoutField field, FieldPosition pos) {
        return Math.max(18.0, effectiveHeightRatio(field, pos) * chequePreviewPane.getHeight());
    }

    private double effectiveWidthRatio(LayoutField field, FieldPosition pos) {
        if (pos.getWidthRatio() > 0) {
            return pos.getWidthRatio();
        }
        return switch (field) {
            case DATE -> 0.19;
            case PAYEE -> 0.66;
            case AMOUNT_NUMBER -> 0.16;
            case AMOUNT_WORDS -> 0.62;
            case SIGNATURE -> 0.22;
            case BANK_LOGO -> 0.18;
            case MICR -> 0.50;
        };
    }

    private double effectiveHeightRatio(LayoutField field, FieldPosition pos) {
        if (pos.getHeightRatio() > 0) {
            return pos.getHeightRatio();
        }
        return switch (field) {
            case SIGNATURE -> 0.16;
            case AMOUNT_NUMBER -> 0.11;
            case DATE, BANK_LOGO -> 0.10;
            case PAYEE, AMOUNT_WORDS -> 0.09;
            case MICR -> 0.08;
        };
    }

    private double parsePositive(String raw, String label) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (Exception ex) {
            throw new IllegalArgumentException(label + " must be a valid number in mm.");
        }
    }

    private String formatMm(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private void persistCurrentLayoutIfPossible() {
        if (currentLayout == null) {
            return;
        }

        String code = selectedBank != null ? safeCode(selectedBank.getBankCode()) : safeCode(fldBankCode.getText());
        if (code.isBlank()) {
            return;
        }

        final BankTemplateLayout layoutToSave = currentLayout.copy();
        new Thread(() -> {
            try {
                layoutByBankCode.put(code, layoutToSave);
                bankService.saveLayouts(layoutByBankCode);
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Layout Save Error", "Unable to save cheque alignment: " + ex.getMessage(), Alert.AlertType.ERROR));
            }
        }, "persist-layout").start();
    }

    private String safeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static final class Delta {
        double x;
        double y;
    }
}
