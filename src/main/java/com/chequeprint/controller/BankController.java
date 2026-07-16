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
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;

import java.io.File;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankController {

    private static final double PREVIEW_PPI = 90.0;

    @FXML
    private ComboBox<Bank> fldBankName;
    @FXML
    private TextField fldBankCode;
    @FXML
    private ComboBox<ChequeSizePreset> cmbChequeSize;
    @FXML
    private ComboBox<String> cmbChequeSizeUnit;
    @FXML
    private Label lblCustomWidth;
    @FXML
    private Label lblCustomHeight;
    @FXML
    private CheckBox chkMicr;
    @FXML
    private CheckBox chkSnapGrid;
    @FXML
    private TextField fldCustomWidth;
    @FXML
    private TextField fldCustomHeight;

    private String currentUnit = "Inches (in)";
    @FXML
    private Button btnSave;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnNewBank;

    @FXML
    private Label lblFormTitle;
    @FXML
    private Label lblPreviewSize;
    @FXML
    private Label lblZoom;
    private double zoomLevel = 1.0;
    @FXML
    private StackPane previewViewport;
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
    private Line guideLineV;
    private Line guideLineH;

    private Bank selectedBank;
    private BankTemplateLayout currentLayout;

    private boolean isUpdatingForm = false;

    @FXML
    public void initialize() {
        setupForm();
        setupPreview();
        setupAdjustmentPanel();
        loadLayouts();
        loadData();
        FxUtils.animateIn(previewViewport, 0);
    }

    private void setupForm() {
        btnDelete.setDisable(true);
        fldBankName.setItems(data);
        fldBankName.setConverter(new StringConverter<Bank>() {
            @Override
            public String toString(Bank bank) {
                return bank == null ? "" : bank.getBankName();
            }

            @Override
            public Bank fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                for (Bank b : data) {
                    if (string.equalsIgnoreCase(b.getBankName())) {
                        return b;
                    }
                }
                Bank b = new Bank();
                b.setBankName(string.trim());
                return b;
            }
        });

        fldBankName.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingForm) {
                return;
            }
            if (newVal != null && newVal.getId() > 0) {
                populateForm(newVal);
            }
        });

        cmbChequeSize.setItems(FXCollections.observableArrayList(ChequeSizePreset.values()));
        cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        chkMicr.setSelected(true);

        fldCustomWidth.setDisable(true);
        fldCustomHeight.setDisable(true);

        if (cmbChequeSizeUnit != null) {
            cmbChequeSizeUnit.setItems(FXCollections.observableArrayList(
                "Inches (in)", "Millimeters (mm)", "Centimeters (cm)", "Pixels (300 DPI)", "Pixels (72 DPI)"
            ));
            cmbChequeSizeUnit.setValue("Inches (in)");
            cmbChequeSizeUnit.setDisable(true);

            cmbChequeSizeUnit.valueProperty().addListener((obs, oldUnit, newUnit) -> {
                if (newUnit == null || newUnit.equals(oldUnit)) {
                    return;
                }
                String unitSuffix = switch (newUnit) {
                    case "Millimeters (mm)" -> "(mm)";
                    case "Centimeters (cm)" -> "(cm)";
                    case "Pixels (300 DPI)", "Pixels (72 DPI)" -> "(px)";
                    default -> "(in)";
                };
                if (lblCustomWidth != null) lblCustomWidth.setText("Custom Width " + unitSuffix);
                if (lblCustomHeight != null) lblCustomHeight.setText("Custom Height " + unitSuffix);

                try {
                    String wText = fldCustomWidth.getText().trim();
                    String hText = fldCustomHeight.getText().trim();
                    if (!wText.isEmpty() && !hText.isEmpty()) {
                        double prevW = Double.parseDouble(wText);
                        double prevH = Double.parseDouble(hText);
                        double inchesW = convertToInches(prevW, oldUnit);
                        double inchesH = convertToInches(prevH, oldUnit);
                        double newW = convertFromInches(inchesW, newUnit);
                        double newH = convertFromInches(inchesH, newUnit);
                        fldCustomWidth.setText(String.format("%.2f", newW));
                        fldCustomHeight.setText(String.format("%.2f", newH));
                    }
                } catch (NumberFormatException ignored) {
                }
                currentUnit = newUnit;
                refreshLayoutForSizeChange();
            });
        }

        cmbChequeSize.valueProperty().addListener((obs, old, preset) -> {
            boolean custom = preset == ChequeSizePreset.CUSTOM;
            fldCustomWidth.setDisable(!custom);
            fldCustomHeight.setDisable(!custom);
            if (cmbChequeSizeUnit != null) {
                cmbChequeSizeUnit.setDisable(!custom);
                if (!custom) {
                    cmbChequeSizeUnit.setValue("Inches (in)");
                    currentUnit = "Inches (in)";
                    if (lblCustomWidth != null) lblCustomWidth.setText("Custom Width (in)");
                    if (lblCustomHeight != null) lblCustomHeight.setText("Custom Height (in)");
                }
            }
            if (!custom) {
                fldCustomWidth.clear();
                fldCustomHeight.clear();
            }
            refreshLayoutForSizeChange();
        });

        fldCustomWidth.textProperty().addListener((obs, o, n) -> refreshLayoutForSizeChange());
        fldCustomHeight.textProperty().addListener((obs, o, n) -> refreshLayoutForSizeChange());

        chkMicr.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        if (chkSnapGrid != null) {
            chkSnapGrid.setSelected(true);
            chkSnapGrid.selectedProperty().addListener((obs, o, n) -> updateGridOverlay());
        }
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

        guideLineV = new Line();
        guideLineV.setStroke(Color.web("#ef4444", 0.8));
        guideLineV.getStrokeDashArray().addAll(4.0, 4.0);
        guideLineV.setVisible(false);
        guideLineV.setManaged(false);

        guideLineH = new Line();
        guideLineH.setStroke(Color.web("#ef4444", 0.8));
        guideLineH.getStrokeDashArray().addAll(4.0, 4.0);
        guideLineH.setVisible(false);
        guideLineH.setManaged(false);

        chequePreviewPane.getChildren().addAll(guideLineV, guideLineH);

        previewViewport.widthProperty().addListener((obs, old, v) -> layoutPreviewPane());
        previewViewport.heightProperty().addListener((obs, old, v) -> layoutPreviewPane());
        updateGridOverlay();
    }

    private void setupAdjustmentPanel() {
        cmbAdjustField.setItems(FXCollections.observableArrayList(LayoutField.values()));
        cmbAdjustField.setValue(LayoutField.PAYEE);
        cmbAdjustField.valueProperty().addListener((obs, old, field) -> {
            loadAdjustmentFields(field);
            updateFieldHighlights();
        });
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
            cmbAdjustField.setValue(field);
            e.consume();
        });
        node.setOnMouseDragged(e -> {
            moveFieldNode(field, node, e, delta);
            e.consume();
        });
        node.setOnMouseReleased(e -> {
            if (guideLineV != null) guideLineV.setVisible(false);
            if (guideLineH != null) guideLineH.setVisible(false);
            persistCurrentLayoutIfPossible();
            e.consume();
        });
    }

    private void moveFieldNode(LayoutField field, Label node, MouseEvent event, Delta delta) {
        if (currentLayout == null) {
            return;
        }

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0) paneW = 720;
        if (paneH <= 0) paneH = 300;

        double nx = node.getLayoutX() + (event.getX() - delta.x);
        double ny = node.getLayoutY() + (event.getY() - delta.y);

        if (chkSnapGrid != null && chkSnapGrid.isSelected()) {
            nx = Math.round(nx / 15.0) * 15.0;
            ny = Math.round(ny / 15.0) * 15.0;
        }

        nx = clamp(nx, 0, Math.max(0, paneW - node.getPrefWidth()));
        ny = clamp(ny, 0, Math.max(0, paneH - node.getPrefHeight()));

        // Guidelines logic (alignment with horizontal/vertical center)
        double centerX = nx + node.getPrefWidth() / 2.0;
        double centerY = ny + node.getPrefHeight() / 2.0;

        if (guideLineV != null) {
            if (Math.abs(centerX - paneW / 2.0) < 6.0) {
                nx = paneW / 2.0 - node.getPrefWidth() / 2.0;
                guideLineV.setStartX(paneW / 2.0);
                guideLineV.setStartY(0);
                guideLineV.setEndX(paneW / 2.0);
                guideLineV.setEndY(paneH);
                guideLineV.setVisible(true);
            } else {
                guideLineV.setVisible(false);
            }
        }

        if (guideLineH != null) {
            if (Math.abs(centerY - paneH / 2.0) < 6.0) {
                ny = paneH / 2.0 - node.getPrefHeight() / 2.0;
                guideLineH.setStartX(0);
                guideLineH.setStartY(paneH / 2.0);
                guideLineH.setEndX(paneW);
                guideLineH.setEndY(paneH / 2.0);
                guideLineH.setVisible(true);
            } else {
                guideLineH.setVisible(false);
            }
        }

        node.setLayoutX(nx);
        node.setLayoutY(ny);

        double xr = paneW <= 0 ? 0 : nx / paneW;
        double yr = paneH <= 0 ? 0 : ny / paneH;
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

        double baseScale = Math.min((vw - 24) / widthPx, (vh - 24) / heightPx);
        baseScale = Math.max(0.25, baseScale);
        double scale = baseScale * zoomLevel;

        chequePreviewPane.setPrefSize(widthPx, heightPx);
        chequePreviewPane.setMinSize(widthPx, heightPx);
        chequePreviewPane.setMaxSize(widthPx, heightPx);
        chequePreviewPane.setScaleX(scale);
        chequePreviewPane.setScaleY(scale);
        chequePreviewPane.getTransforms().clear();

        if (lblZoom != null) {
            lblZoom.setText(Math.round(zoomLevel * 100) + "%");
        }

        refreshPreview();
    }

    private void refreshPreview() {
        if (currentLayout == null) {
            return;
        }
        currentLayout.ensureAllFields();

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0) paneW = 720;
        if (paneH <= 0) paneH = 300;

        for (Map.Entry<LayoutField, Label> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            Label node = entry.getValue();
            FieldPosition pos = currentLayout.get(field);
            double x = pos.getXRatio() * paneW;
            double y = pos.getYRatio() * paneH;
            double w = fieldWidthPx(field, pos);
            double h = fieldHeightPx(field, pos);
            node.setPrefSize(w, h);
            node.setMinSize(w, h);
            node.setMaxSize(w, h);
            x = clamp(x, 0, Math.max(0, paneW - w));
            y = clamp(y, 0, Math.max(0, paneH - h));
            node.setLayoutX(x);
            node.setLayoutY(y);
            node.setVisible(field != LayoutField.MICR || chkMicr.isSelected());
        }

        lblPreviewSize.setText(String.format("Preview Size: %.2f x %.2f inches", currentLayout.getWidthInches(), currentLayout.getHeightInches()));
        loadAdjustmentFields(cmbAdjustField.getValue());
        updateFieldHighlights();
    }

    @FXML
    private void onZoomIn() {
        if (zoomLevel < 2.5) {
            zoomLevel += 0.1;
            layoutPreviewPane();
        }
    }

    @FXML
    private void onZoomOut() {
        if (zoomLevel > 0.4) {
            zoomLevel -= 0.1;
            layoutPreviewPane();
        }
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

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            if (fldBankCode.getScene() != null && fldBankCode.getScene().getRoot() != null) {
                fldBankCode.getScene().getRoot().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
            }
            btnSave.setDisable(loading);
            btnDelete.setDisable(loading || selectedBank == null);
        });
    }

    private void loadData() {
        setLoading(true);
        new Thread(() -> {
            try {
                List<Bank> list = bankService.getAll();
                Platform.runLater(() -> data.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Load Error", e.getMessage(), Alert.AlertType.ERROR));
            } finally {
                setLoading(false);
            }
        }, "load-banks").start();
    }

    @FXML
    private void onSave() {
        String name = "";
        if (fldBankName.getValue() != null) {
            name = fldBankName.getValue().getBankName();
        }
        if (name == null || name.trim().isEmpty()) {
            name = fldBankName.getEditor().getText().trim();
        } else {
            name = name.trim();
        }
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

        setLoading(true);
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
            } finally {
                setLoading(false);
            }
        }, "save-bank").start();
    }

    @FXML
    private void onDelete() {
        Bank sel = selectedBank;
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
                setLoading(true);
                new Thread(() -> {
                    try {
                        bankService.delete(sel, layoutByBankCode);
                        Platform.runLater(() -> {
                            clearForm();
                            loadData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Delete Error", e.getMessage(), Alert.AlertType.ERROR));
                    } finally {
                        setLoading(false);
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
        btnDelete.setDisable(false);
        if (btnNewBank != null) {
            btnNewBank.setVisible(true);
            btnNewBank.setManaged(true);
        }
        if (btnClear != null) {
            btnClear.setVisible(true);
            btnClear.setManaged(true);
        }

        isUpdatingForm = true;
        try {
            fldBankName.setValue(bank);
        } finally {
            isUpdatingForm = false;
        }

        fldBankCode.setText(bank.getBankCode());
        chkMicr.setSelected(bank.isMicr());

        ChequeSizePreset preset = ChequeSizePreset.fromValue(bank.getChequeSize());
        cmbChequeSize.setValue(preset);

        if (preset == ChequeSizePreset.CUSTOM) {
            BankTemplateLayout sizeLayout = ChequeSizeCodec.decodeLayout(bank.getChequeSize());
            if (cmbChequeSizeUnit != null) {
                cmbChequeSizeUnit.setDisable(false);
                cmbChequeSizeUnit.setValue("Inches (in)");
                currentUnit = "Inches (in)";
            }
            if (lblCustomWidth != null) lblCustomWidth.setText("Custom Width (in)");
            if (lblCustomHeight != null) lblCustomHeight.setText("Custom Height (in)");
            fldCustomWidth.setText(String.format("%.2f", sizeLayout.getWidthInches()));
            fldCustomHeight.setText(String.format("%.2f", sizeLayout.getHeightInches()));
            fldCustomWidth.setDisable(false);
            fldCustomHeight.setDisable(false);
        } else {
            if (cmbChequeSizeUnit != null) {
                cmbChequeSizeUnit.setDisable(true);
                cmbChequeSizeUnit.setValue("Inches (in)");
                currentUnit = "Inches (in)";
            }
            if (lblCustomWidth != null) lblCustomWidth.setText("Custom Width (in)");
            if (lblCustomHeight != null) lblCustomHeight.setText("Custom Height (in)");
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
        btnDelete.setDisable(true);
        if (btnNewBank != null) {
            btnNewBank.setVisible(false);
            btnNewBank.setManaged(false);
        }
        if (btnClear != null) {
            btnClear.setText("Clear");
            btnClear.setVisible(true);
            btnClear.setManaged(true);
            // Ensure style class is btn-secondary
            btnClear.getStyleClass().remove("btn-primary");
            if (!btnClear.getStyleClass().contains("btn-secondary")) {
                btnClear.getStyleClass().add("btn-secondary");
            }
        }
        
        isUpdatingForm = true;
        try {
            fldBankName.setValue(null);
            fldBankName.getEditor().clear();
        } finally {
            isUpdatingForm = false;
        }
        fldBankCode.clear();
        chkMicr.setSelected(true);
        cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        fldCustomWidth.clear();
        fldCustomHeight.clear();

        currentLayout = new BankTemplateLayout(ChequeSizePreset.STANDARD.getWidthInches(), ChequeSizePreset.STANDARD.getHeightInches());
        layoutPreviewPane();
        refreshPreview();
    }

    private void refreshLayoutForSizeChange() {
        BankTemplateLayout sizeLayout = buildLayoutFromFormSizeSilently();
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

    private BankTemplateLayout buildLayoutFromFormSizeSilently() {
        ChequeSizePreset preset = cmbChequeSize.getValue();
        if (preset == null) {
            preset = ChequeSizePreset.STANDARD;
        }

        if (preset != ChequeSizePreset.CUSTOM) {
            return new BankTemplateLayout(preset.getWidthInches(), preset.getHeightInches());
        }

        try {
            double rawW = Double.parseDouble(fldCustomWidth.getText().trim());
            double rawH = Double.parseDouble(fldCustomHeight.getText().trim());
            if (rawW <= 0 || rawH <= 0) {
                return null;
            }
            double w = convertToInches(rawW, cmbChequeSizeUnit.getValue());
            double h = convertToInches(rawH, cmbChequeSizeUnit.getValue());
            return new BankTemplateLayout(w, h);
        } catch (Exception ex) {
            return null;
        }
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
            double rawW = Double.parseDouble(fldCustomWidth.getText().trim());
            double rawH = Double.parseDouble(fldCustomHeight.getText().trim());
            if (rawW <= 0 || rawH <= 0) {
                throw new NumberFormatException("Size must be positive.");
            }
            double w = convertToInches(rawW, cmbChequeSizeUnit.getValue());
            double h = convertToInches(rawH, cmbChequeSizeUnit.getValue());
            return new BankTemplateLayout(w, h);
        } catch (NumberFormatException ex) {
            showAlert("Validation", "Enter valid custom width and height.", Alert.AlertType.WARNING);
            return null;
        }
    }

    private double convertFromInches(double inches, String toUnit) {
        if (toUnit == null) return inches;
        return switch (toUnit) {
            case "Millimeters (mm)" -> inches * 25.4;
            case "Centimeters (cm)" -> inches * 2.54;
            case "Pixels (300 DPI)" -> inches * 300.0;
            case "Pixels (72 DPI)" -> inches * 72.0;
            default -> inches; // Inches (in)
        };
    }

    private double convertToInches(double value, String fromUnit) {
        if (fromUnit == null) return value;
        return switch (fromUnit) {
            case "Millimeters (mm)" -> value / 25.4;
            case "Centimeters (cm)" -> value / 2.54;
            case "Pixels (300 DPI)" -> value / 300.0;
            case "Pixels (72 DPI)" -> value / 72.0;
            default -> value; // Inches (in)
        };
    }

    private Bank buildDraftBank() {
        Bank bank = new Bank();
        String name = "";
        if (fldBankName.getValue() != null) {
            name = fldBankName.getValue().getBankName();
        }
        if (name == null || name.trim().isEmpty()) {
            name = fldBankName.getEditor().getText().trim();
        } else {
            name = name.trim();
        }
        bank.setBankName(name);
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
        double w = chequePreviewPane.getPrefWidth();
        if (w <= 0) w = 720;
        return Math.max(24.0, effectiveWidthRatio(field, pos) * w);
    }

    private double fieldHeightPx(LayoutField field, FieldPosition pos) {
        double h = chequePreviewPane.getPrefHeight();
        if (h <= 0) h = 300;
        return Math.max(18.0, effectiveHeightRatio(field, pos) * h);
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

    private void updateGridOverlay() {
        if (chkSnapGrid == null) {
            return;
        }
        boolean showGrid = chkSnapGrid.isSelected();

        String watermarkStyles =
            "-fx-background-color: #f2f7fc, " +
            "linear-gradient(to bottom, #1e3a8a 0px, #1e3a8a 6px, transparent 6px, transparent 100%), " +
            "linear-gradient(to top, #1e3a8a 0px, #1e3a8a 6px, transparent 6px, transparent 100%), " +
            "repeating-linear-gradient(45deg, rgba(37,99,235,0.02) 0px, rgba(37,99,235,0.02) 2px, transparent 2px, transparent 16px), " +
            "repeating-linear-gradient(-45deg, rgba(37,99,235,0.02) 0px, rgba(37,99,235,0.02) 2px, transparent 2px, transparent 16px)";

        if (showGrid) {
            chequePreviewPane.setStyle(
                watermarkStyles + ", " +
                "linear-gradient(from 0px 0px to 15px 0px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px), " +
                "linear-gradient(from 0px 0px to 0px 15px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px); " +
                "-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"
            );
        } else {
            chequePreviewPane.setStyle(
                watermarkStyles + "; " +
                "-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"
            );
        }
    }

    private void updateFieldHighlights() {
        LayoutField selected = cmbAdjustField.getValue();
        for (Map.Entry<LayoutField, Label> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            Label node = entry.getValue();

            String baseStyle = switch (field) {
                case BANK_LOGO -> "-fx-background-color:rgba(239,246,255,0.85); -fx-border-color:#3b82f6;";
                case DATE -> "-fx-background-color:rgba(248,250,252,0.85); -fx-border-color:#64748b;";
                case PAYEE -> "-fx-background-color:rgba(248,250,252,0.85); -fx-border-color:#64748b;";
                case AMOUNT_NUMBER -> "-fx-background-color:rgba(254,252,232,0.85); -fx-border-color:#ca8a04;";
                case AMOUNT_WORDS -> "-fx-background-color:rgba(248,250,252,0.85); -fx-border-color:#64748b;";
                case SIGNATURE -> "-fx-background-color:rgba(248,250,252,0.85); -fx-border-color:#64748b;";
                case MICR -> "-fx-background-color:rgba(241,255,249,0.85); -fx-border-color:#10b981;";
            };

            if (field == selected) {
                node.setStyle(baseStyle + " -fx-font-size:11px; -fx-font-weight:700; -fx-border-color:#2563eb; -fx-border-style:dashed; -fx-border-width:2px; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.35), 6, 0, 0, 0);");
            } else {
                node.setStyle(baseStyle + " -fx-font-size:11px; -fx-font-weight:600; -fx-border-width:1px;");
            }
        }
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

