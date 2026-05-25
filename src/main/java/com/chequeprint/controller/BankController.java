package com.chequeprint.controller;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.util.BankTemplateLayoutStore;
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

    private final BankDAO dao = new BankDAO();
    private final BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();
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
        createFieldNode(LayoutField.BANK_LOGO, "BANK LOGO", "-fx-background-color:#eff6ff; -fx-border-color:#3b82f6;", 110, 28);
        createFieldNode(LayoutField.DATE, "DATE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;", 90, 24);
        createFieldNode(LayoutField.PAYEE, "PAY TO", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;", 110, 24);
        createFieldNode(LayoutField.AMOUNT_NUMBER, "AMOUNT #", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;", 120, 24);
        createFieldNode(LayoutField.AMOUNT_WORDS, "AMOUNT WORDS", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;", 140, 24);
        createFieldNode(LayoutField.SIGNATURE, "SIGNATURE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;", 120, 24);
        createFieldNode(LayoutField.MICR, "MICR AREA", "-fx-background-color:#f1f5f9; -fx-border-color:#334155;", 150, 24);

        previewViewport.widthProperty().addListener((obs, old, v) -> layoutPreviewPane());
        previewViewport.heightProperty().addListener((obs, old, v) -> layoutPreviewPane());
    }

    private void createFieldNode(LayoutField field, String text, String style, double w, double h) {
        Label label = new Label(text);
        label.setPadding(new Insets(2, 5, 2, 5));
        label.setStyle(style + " -fx-font-size:11px; -fx-font-weight:600;");
        label.setPrefSize(w, h);
        label.setMinSize(w, h);
        label.setMaxSize(w, h);
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
            x = clamp(x, 0, Math.max(0, chequePreviewPane.getWidth() - node.getWidth()));
            y = clamp(y, 0, Math.max(0, chequePreviewPane.getHeight() - node.getHeight()));
            node.setLayoutX(x);
            node.setLayoutY(y);
            node.setVisible(field != LayoutField.MICR || chkMicr.isSelected());
        }

        lblPreviewSize.setText(String.format("Preview Size: %.2f x %.2f inches", currentLayout.getWidthInches(), currentLayout.getHeightInches()));
    }

    private void loadLayouts() {
        layoutByBankCode.clear();
        layoutByBankCode.putAll(layoutStore.loadAll());
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<Bank> list = dao.findAll();
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

        try {
            if (selectedBank == null) {
                Bank bank = new Bank(name, code, encodedSize, chkMicr.isSelected());
                dao.insert(bank);
            } else {
                String oldCode = safeCode(selectedBank.getBankCode());
                selectedBank.setBankName(name);
                selectedBank.setBankCode(code);
                selectedBank.setChequeSize(encodedSize);
                selectedBank.setMicr(chkMicr.isSelected());
                dao.update(selectedBank);
                if (!oldCode.equals(code)) {
                    BankTemplateLayout moved = layoutByBankCode.remove(oldCode);
                    if (moved != null) {
                        layoutByBankCode.put(code, moved);
                    }
                }
            }

            layoutByBankCode.put(code, currentLayout != null ? currentLayout.copy() : formLayout.copy());
            layoutStore.saveAll(layoutByBankCode);

            clearForm();
            loadData();
            showAlert("Success", "Bank template saved successfully.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Save Error", e.getMessage(), Alert.AlertType.ERROR);
        }
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
                try {
                    dao.delete(sel.getId());
                    layoutByBankCode.remove(safeCode(sel.getBankCode()));
                    layoutStore.saveAll(layoutByBankCode);
                    clearForm();
                    loadData();
                } catch (Exception e) {
                    showAlert("Delete Error", e.getMessage(), Alert.AlertType.ERROR);
                }
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
