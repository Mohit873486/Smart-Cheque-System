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
import javafx.concurrent.Task;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chequeprint.service.ApiService;
import com.chequeprint.model.BankAccount;
import javafx.scene.control.TableView;

public class BankController {

    private final ApiService apiService = new ApiService();
    @FXML private TableView<Bank> bankTable;
    @FXML private TableView<BankAccount> accountTable;
    @FXML private Button btnAddAccount;
    @FXML private VBox emptyState;
    @FXML private VBox loadingSpinner;
    @FXML private VBox previewEmptyState;
    @FXML private VBox chequePreviewCard;
    @FXML private Label previewBankName;
    @FXML private Label previewBankCode;
    @FXML private Label previewHolderName;
    @FXML private Label previewAccountNumber;
    @FXML private Label previewMicr;
    @FXML private VBox previewLoading;
    @FXML private Button btnEditTemplate;

    private static final double PREVIEW_PPI = 90.0;
    private final Map<Long, BankTemplateLayout> bankTemplateMap = new java.util.concurrent.ConcurrentHashMap<>();

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
    private LayoutField selectedLayoutField = LayoutField.PAYEE;

    private LayoutField getSelectedField() {
        if (cmbAdjustField != null && cmbAdjustField.getValue() != null) {
            return cmbAdjustField.getValue();
        }
        return selectedLayoutField;
    }

    private void setSelectedField(LayoutField field) {
        this.selectedLayoutField = field;
        if (cmbAdjustField != null && cmbAdjustField.getValue() != field) {
            cmbAdjustField.setValue(field);
        }
        updateFieldHighlights();
        if (field != null) {
            loadAdjustmentFields(field);
            StackPane node = fieldNodes.get(field);
            if (node != null) {
                updateHUD(field, node);
            }
        }
    }
    @FXML
    private TextField fldAdjustLeft;
    @FXML
    private TextField fldAdjustTop;
    @FXML
    private TextField fldAdjustWidth;
    @FXML
    private TextField fldAdjustHeight;
    @FXML
    private ComboBox<String> cmbFontFamily;
    @FXML
    private TextField fldFontSize;

    // Canva specific fields
    @FXML
    private CheckBox chkShowGrid;
    @FXML
    private CheckBox chkShowRulers;
    @FXML
    private Label lblActiveLayerName;
    @FXML
    private GridPane inspectorGrid;
    @FXML
    private VBox alignmentPanel;
    @FXML
    private Label lblCoordinatesHUD;
    @FXML
    private Button layerDate;
    @FXML
    private Button layerPayee;
    @FXML
    private Button layerAmountNumber;
    @FXML
    private Button layerAmountWords;
    @FXML
    private Button layerSignature;
    @FXML
    private Button layerBankLogo;
    @FXML
    private Button layerMicr;

    private final BankService bankService = new BankService();
    private final ObservableList<Bank> bankList = FXCollections.observableArrayList();
    private final ObservableList<Bank> data = FXCollections.observableArrayList();
    private final ObservableList<BankAccount> accountData = FXCollections.observableArrayList();

    private final Map<String, BankTemplateLayout> layoutByBankCode = new HashMap<>();
    private final Map<LayoutField, StackPane> fieldNodes = new EnumMap<>(LayoutField.class);
    private Line guideLineV;
    private Line guideLineH;

    private Bank selectedBank;
    private BankTemplateLayout currentLayout;

    private boolean isUpdatingForm = false;

    @FXML
    private void onAddAccount() {
        // TODO: Implement adding an account flow or switch tabs
    }

    @FXML
    private void onEditTemplate() {
        // Switch to the template designer tab
        if (fldBankName != null && fldBankName.getScene() != null) {
            javafx.scene.control.TabPane tabPane = (javafx.scene.control.TabPane) fldBankName.getScene().getRoot();
            if (tabPane != null && tabPane.getTabs().size() > 1) {
                tabPane.getSelectionModel().select(1);
            }
        }
    }

    @FXML
    public void initialize() {
        setupForm();
        setupPreview();
        setupAdjustmentPanel();
        setupAccountTableListener();
        loadLayouts();
        loadData();
        loadBankAccounts();
        clearForm(); // Ensures default layout coordinates are applied at startup
        FxUtils.animateIn(previewViewport, 0);
    }

    private void setupAccountTableListener() {
        if (accountTable != null) {
            accountTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    if (previewEmptyState != null) previewEmptyState.setVisible(true);
                    if (chequePreviewCard != null) chequePreviewCard.setVisible(false);
                    if (btnEditTemplate != null) btnEditTemplate.setDisable(true);
                } else {
                    if (previewEmptyState != null) previewEmptyState.setVisible(false);
                    if (chequePreviewCard != null) chequePreviewCard.setVisible(true);
                    if (btnEditTemplate != null) btnEditTemplate.setDisable(false);

                    if (previewBankName != null) previewBankName.setText(newVal.getBankName());
                    if (previewBankCode != null) previewBankCode.setText(newVal.getIfscCode() != null ? newVal.getIfscCode() : "");
                    if (previewHolderName != null) previewHolderName.setText(newVal.getAccountHolderName());
                    if (previewAccountNumber != null) previewAccountNumber.setText(newVal.getAccountNumber());
                    
                    // Default micr for preview if applicable
                    if (previewMicr != null) {
                        previewMicr.setText("⑈" + newVal.getAccountNumber() + "⑈ 000000000 ⑈00⑈");
                    }
                }
            });
        }
    }

    private void setupForm() {
        if (cmbFontFamily != null) {
            cmbFontFamily.setItems(FXCollections.observableArrayList("Arial", "Courier New", "Consolas", "Times New Roman", "Verdana", "Tahoma"));
            cmbFontFamily.setValue("Arial");
            cmbFontFamily.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && getSelectedField() != null) {
                    applySelectedFieldFont(getSelectedField(), newVal, getSelectedFontSize());
                }
            });
        }
        if (fldFontSize != null) {
            fldFontSize.setText("12");
            fldFontSize.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && getSelectedField() != null) {
                    try {
                        int size = Integer.parseInt(newVal.trim());
                        String family = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue() : "Arial";
                        applySelectedFieldFont(getSelectedField(), family, size);
                    } catch (Exception ignored) {}
                }
            });
        }
        if (btnDelete != null) btnDelete.setDisable(true);
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
            if (newVal != null && newVal.getId() != null && newVal.getId() > 0) {
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
        createFieldNode(LayoutField.BANK_LOGO, "BANK LOGO", "-fx-background-color:#eff6ff; -fx-border-color:#3b82f6;");
        createFieldNode(LayoutField.DATE, "DATE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.PAYEE, "PAYEE", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.AMOUNT_NUMBER, "AMOUNT NUMBER", "-fx-background-color:#fefce8; -fx-border-color:#ca8a04;");
        createFieldNode(LayoutField.AMOUNT_WORDS, "AMOUNT WORDS", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.SIGNATURE, "SIGNATURE AREA", "-fx-background-color:#f8fafc; -fx-border-color:#64748b;");
        createFieldNode(LayoutField.MICR, "MICR LINE", "-fx-background-color:#f1f5f9; -fx-border-color:#334155;");

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
        if (cmbAdjustField != null) {
            cmbAdjustField.setItems(FXCollections.observableArrayList(LayoutField.values()));
            cmbAdjustField.setValue(LayoutField.PAYEE);
            cmbAdjustField.valueProperty().addListener((obs, old, field) -> {
                if (field != null) {
                    setSelectedField(field);
                }
            });
        }
        setSelectedField(LayoutField.PAYEE);
    }

    private void createFieldNode(LayoutField field, String text, String style) {
        StackPane node = new StackPane();
        node.setPadding(new Insets(2, 5, 2, 5));
        node.setCursor(Cursor.MOVE);
        
        Label label = new Label(text);
        label.setStyle("-fx-font-size:11px; -fx-font-weight:600; -fx-text-fill: #1e293b;");
        node.getChildren().add(label);
        
        javafx.scene.shape.Circle resizeHandle = new javafx.scene.shape.Circle(4.5, Color.web("#2563eb"));
        resizeHandle.setStroke(Color.WHITE);
        resizeHandle.setStrokeWidth(1.5);
        resizeHandle.setCursor(Cursor.SE_RESIZE);
        resizeHandle.setManaged(false);
        resizeHandle.setVisible(false);
        
        node.getChildren().add(resizeHandle);
        
        node.widthProperty().addListener((obs, o, w) -> resizeHandle.setLayoutX(w.doubleValue() - 4.5));
        node.heightProperty().addListener((obs, o, h) -> resizeHandle.setLayoutY(h.doubleValue() - 4.5));
        
        enableDragAndResize(field, node, resizeHandle);
        fieldNodes.put(field, node);
        chequePreviewPane.getChildren().add(node);
    }
    
    private void enableDragAndResize(LayoutField field, StackPane node, javafx.scene.shape.Circle resizeHandle) {
        final Delta dragDelta = new Delta();
        
        node.setOnMousePressed(e -> {
            if (e.getTarget() == resizeHandle) {
                return;
            }
            dragDelta.x = e.getX();
            dragDelta.y = e.getY();
            setSelectedField(field);
            e.consume();
        });
        
        node.setOnMouseDragged(e -> {
            if (e.getTarget() == resizeHandle) {
                return;
            }
            moveFieldNode(field, node, e, dragDelta);
            e.consume();
        });
        
        node.setOnMouseReleased(e -> {
            if (guideLineV != null) guideLineV.setVisible(false);
            if (guideLineH != null) guideLineH.setVisible(false);
            persistCurrentLayoutIfPossible();
            e.consume();
        });
        
        final Delta resizeDelta = new Delta();
        resizeHandle.setOnMousePressed(e -> {
            resizeDelta.x = e.getScreenX();
            resizeDelta.y = e.getScreenY();
            dragDelta.x = node.getPrefWidth();
            dragDelta.y = node.getPrefHeight();
            setSelectedField(field);
            e.consume();
        });
        
        resizeHandle.setOnMouseDragged(e -> {
            double dx = e.getScreenX() - resizeDelta.x;
            double dy = e.getScreenY() - resizeDelta.y;
            
            double newW = dragDelta.x + dx;
            double newH = dragDelta.y + dy;
            
            if (chkSnapGrid != null && chkSnapGrid.isSelected()) {
                newW = Math.round(newW / 15.0) * 15.0;
                newH = Math.round(newH / 15.0) * 15.0;
            }
            
            newW = Math.max(30.0, newW);
            newH = Math.max(15.0, newH);
            
            double paneW = chequePreviewPane.getPrefWidth();
            double paneH = chequePreviewPane.getPrefHeight();
            if (paneW <= 0) paneW = 720;
            if (paneH <= 0) paneH = 300;
            
            double maxW = paneW - node.getLayoutX();
            double maxH = paneH - node.getLayoutY();
            newW = Math.min(newW, maxW);
            newH = Math.min(newH, maxH);
            
            node.setPrefSize(newW, newH);
            node.setMinSize(newW, newH);
            node.setMaxSize(newW, newH);
            
            FieldPosition pos = currentLayout.get(field);
            currentLayout.setFieldLayout(field, 
                pos.getXRatio(), pos.getYRatio(), 
                newW / paneW, newH / paneH
            );
            
            loadAdjustmentFields(field);
            updateHUD(field, node);
            e.consume();
        });
        
        resizeHandle.setOnMouseReleased(e -> {
            persistCurrentLayoutIfPossible();
            e.consume();
        });
    }

    private void updateHUD(LayoutField field, StackPane node) {
        if (lblCoordinatesHUD != null && currentLayout != null) {
            double widthMm = currentLayout.getWidthInches() * 25.4;
            double heightMm = currentLayout.getHeightInches() * 25.4;
            double paneW = chequePreviewPane.getPrefWidth();
            double paneH = chequePreviewPane.getPrefHeight();
            if (paneW <= 0) paneW = 720;
            if (paneH <= 0) paneH = 300;

            lblCoordinatesHUD.setText(String.format("Active Field: %s | X: %.1f mm, Y: %.1f mm | W: %.1f mm, H: %.1f mm",
                field.name(),
                (node.getLayoutX() / paneW) * widthMm,
                (node.getLayoutY() / paneH) * heightMm,
                (node.getPrefWidth() / paneW) * widthMm,
                (node.getPrefHeight() / paneH) * heightMm
            ));
        }
    }

    private void moveFieldNode(LayoutField field, StackPane node, MouseEvent event, Delta delta) {
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
        if (field == getSelectedField()) {
            loadAdjustmentFields(field);
        }
        updateHUD(field, node);
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

        for (Map.Entry<LayoutField, StackPane> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            StackPane node = entry.getValue();
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
        loadAdjustmentFields(getSelectedField());
        updateFieldHighlights();

        LayoutField selected = getSelectedField();
        if (selected != null) {
            StackPane node = fieldNodes.get(selected);
            if (node != null) {
                updateHUD(selected, node);
            }
        }
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

    private void loadBankAccounts() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(true);
            loadingSpinner.setManaged(true);
        }
        if (emptyState != null) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }

        Task<List<BankAccount>> task = new Task<>() {
            @Override
            protected List<BankAccount> call() throws Exception {
                return apiService.getBankAccounts();
            }
        };

        task.setOnSucceeded(e -> {
            List<BankAccount> accounts = task.getValue();
            accountData.setAll(accounts);
            if (accountTable != null) {
                accountTable.setItems(accountData);
                accountTable.refresh();
            }
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(false);
                loadingSpinner.setManaged(false);
            }
            if (emptyState != null) {
                boolean isEmpty = accounts.isEmpty();
                emptyState.setVisible(isEmpty);
                emptyState.setManaged(isEmpty);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(false);
                loadingSpinner.setManaged(false);
            }
            showAlert("API Error", "Failed to load bank accounts: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        Thread thread = new Thread(task, "load-accounts-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadData() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(true);
            loadingSpinner.setManaged(true);
        }
        setLoading(true);

        Task<List<Bank>> task = new Task<>() {
            @Override
            protected List<Bank> call() throws Exception {
                return bankService.getBanks();
            }
        };

        task.setOnSucceeded(e -> {
            List<Bank> list = task.getValue();
            bankList.setAll(list);
            data.setAll(list);
            if (bankTable != null) {
                bankTable.setItems(bankList);
                bankTable.refresh();
            }
            if (fldBankName != null) {
                fldBankName.setItems(data);
            }
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(false);
                loadingSpinner.setManaged(false);
            }
            setLoading(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(false);
                loadingSpinner.setManaged(false);
            }
            setLoading(false);
            showAlert("Load Error", ex.getMessage(), Alert.AlertType.ERROR);
        });

        Thread thread = new Thread(task, "load-banks-task");
        thread.setDaemon(true);
        thread.start();
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

        // 1. Empty Fields Validation
        if (name.isEmpty() && code.isEmpty()) {
            showAlert("Validation Error", "Bank name and bank code / account number are required.", Alert.AlertType.WARNING);
            return;
        }
        if (name.isEmpty()) {
            showAlert("Validation Error", "Please enter a valid Bank Name.", Alert.AlertType.WARNING);
            return;
        }
        if (code.isEmpty()) {
            showAlert("Validation Error", "Please enter a valid Bank Code / Account Number.", Alert.AlertType.WARNING);
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

        // Construct valid IFSC Code (11 alphanumeric characters matching ^[A-Z]{4}0[A-Z0-9]{6}$)
        String formattedIfsc = formatValidIfsc(code);

        setLoading(true);
        new Thread(() -> {
            try {
                // 2. Call Bank Template Local/REST Service
                bankService.save(finalBank, layoutToSave, layoutByBankCode);

                // 3. Call POST API for Bank Account
                BankAccount account = new BankAccount();
                account.setBankName(finalBank.getBankName());
                account.setAccountNumber(finalBank.getBankCode());
                account.setAccountHolderName(finalBank.getBankName());
                account.setIfsc(formattedIfsc);
                account.setIfscCode(formattedIfsc);
                account.setBranch("Main Branch");
                account.setBranchName("Main Branch");
                account.setBalance(java.math.BigDecimal.ZERO);

                try {
                    apiService.saveBankAccount(account);
                } catch (Exception apiEx) {
                    System.err.println("API Bank Account Save Warning: " + apiEx.getMessage());
                }

                // 4. Save Template Fields to REST API (POST /api/template/fields)
                Long templateId = finalBank.getId() != null ? finalBank.getId().longValue() : 1L;
                saveTemplateFieldsToApi(templateId);

                // 5. Show Success Alert & Refresh Table
                Platform.runLater(() -> {
                    clearForm();
                    loadData();
                    loadBankAccounts();
                    showAlert("Success", "Bank template & field positions saved successfully!", Alert.AlertType.INFORMATION);
                });
            } catch (Exception e) {
                // 5. API / System Error Handling
                Platform.runLater(() -> showAlert("Save Error", "Failed to save bank: " + e.getMessage(), Alert.AlertType.ERROR));
            } finally {
                setLoading(false);
            }
        }, "save-bank").start();
    }

    private String formatValidIfsc(String code) {
        if (code != null && code.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            return code;
        }
        String prefix = (code != null && code.length() >= 4) ? code.substring(0, 4).replaceAll("[^A-Z]", "A") : "SBIN";
        while (prefix.length() < 4) {
            prefix += "A";
        }
        return prefix + "0001234";
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
                            selectedBank = null;
                            clearForm();
                            loadData();
                            loadBankAccounts();
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
        if (bank == null) {
            clearOldUI();
            return;
        }

        // 1. Clear old UI canvas & inspector selection state
        clearOldUI();

        selectedBank = bank;
        Long bankId = bank.getId() != null ? bank.getId().longValue() : 1L;

        // 2. Load new template from Map cache or REST API
        loadNewTemplate(bankId, bank);

        lblFormTitle.setText("Edit Bank Template (" + bank.getBankName() + ")");
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
        if (chequePreviewPane == null) {
            return;
        }

        boolean showGrid = chkShowGrid != null && chkShowGrid.isSelected();
        boolean showRulers = chkShowRulers != null && chkShowRulers.isSelected();

        StringBuilder style = new StringBuilder();
        style.append("-fx-border-color: #475569; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-border-radius: 6px; ");

        if (showGrid) {
            style.append("-fx-background-color: #ffffff, ");
            style.append("linear-gradient(from 0px 0px to 15px 0px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px), ");
            style.append("linear-gradient(from 0px 0px to 0px 15px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px); ");
        } else {
            style.append("-fx-background-color: #ffffff; ");
        }

        if (showRulers) {
            style.append("-fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.15), 10, 0, 0, 0); ");
        }

        chequePreviewPane.setStyle(style.toString());
    }

    @FXML
    private void onToggleGrid() {
        updateGridOverlay();
    }

    @FXML
    private void onToggleRulers() {
        updateGridOverlay();
    }

    private void updateFieldHighlights() {
        LayoutField selected = getSelectedField();

        if (layerDate != null) {
            setLayerButtonSelected(layerDate, selected == LayoutField.DATE);
            setLayerButtonSelected(layerPayee, selected == LayoutField.PAYEE);
            setLayerButtonSelected(layerAmountNumber, selected == LayoutField.AMOUNT_NUMBER);
            setLayerButtonSelected(layerAmountWords, selected == LayoutField.AMOUNT_WORDS);
            setLayerButtonSelected(layerSignature, selected == LayoutField.SIGNATURE);
            setLayerButtonSelected(layerBankLogo, selected == LayoutField.BANK_LOGO);
            setLayerButtonSelected(layerMicr, selected == LayoutField.MICR);
        }

        if (lblActiveLayerName != null) {
            lblActiveLayerName.setText(selected == null ? "None" : selected.name());
        }
        if (inspectorGrid != null) {
            inspectorGrid.setDisable(selected == null);
        }
        if (alignmentPanel != null) {
            alignmentPanel.setDisable(selected == null);
        }

        for (Map.Entry<LayoutField, StackPane> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            StackPane node = entry.getValue();

            javafx.scene.shape.Circle resizeHandle = null;
            for (javafx.scene.Node child : node.getChildren()) {
                if (child instanceof javafx.scene.shape.Circle) {
                    resizeHandle = (javafx.scene.shape.Circle) child;
                    break;
                }
            }

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
                node.setStyle(baseStyle + " -fx-border-color:#2563eb; -fx-border-style:dashed; -fx-border-width:2px; -fx-background-radius:4; -fx-border-radius:4; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.35), 6, 0, 0, 0);");
                if (resizeHandle != null) resizeHandle.setVisible(true);
            } else {
                node.setStyle(baseStyle + " -fx-border-width:1px; -fx-background-radius:4; -fx-border-radius:4;");
                if (resizeHandle != null) resizeHandle.setVisible(false);
            }
        }
    }

    private void setLayerButtonSelected(Button button, boolean isSelected) {
        if (button == null) return;
        button.getStyleClass().removeAll("btn-primary", "btn-secondary");
        if (isSelected) {
            button.getStyleClass().add("btn-primary");
            button.setStyle("-fx-alignment: center-left; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            button.getStyleClass().add("btn-secondary");
            button.setStyle("-fx-alignment: center-left; -fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: normal;");
        }
    }

    @FXML
    private void onSelectLayerDate() { setSelectedField(LayoutField.DATE); }
    @FXML
    private void onSelectLayerPayee() { setSelectedField(LayoutField.PAYEE); }
    @FXML
    private void onSelectLayerAmountNumber() { setSelectedField(LayoutField.AMOUNT_NUMBER); }
    @FXML
    private void onSelectLayerAmountWords() { setSelectedField(LayoutField.AMOUNT_WORDS); }
    @FXML
    private void onSelectLayerSignature() { setSelectedField(LayoutField.SIGNATURE); }
    @FXML
    private void onSelectLayerBankLogo() { setSelectedField(LayoutField.BANK_LOGO); }
    @FXML
    private void onSelectLayerMicr() { setSelectedField(LayoutField.MICR); }

    @FXML
    private void onAlignLeft() { alignSelected(0.0, -1); }
    @FXML
    private void onAlignRight() { alignSelected(1.0, -1); }
    @FXML
    private void onCenterHorizontal() { alignSelected(0.5, -1); }
    @FXML
    private void onAlignTop() { alignSelected(-1, 0.0); }
    @FXML
    private void onAlignBottom() { alignSelected(-1, 1.0); }
    @FXML
    private void onCenterVertical() { alignSelected(-1, 0.5); }

    private void alignSelected(double targetX, double targetY) {
        if (currentLayout == null) return;
        LayoutField field = getSelectedField();
        if (field == null) return;

        StackPane node = fieldNodes.get(field);
        if (node == null) return;

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0) paneW = 720;
        if (paneH <= 0) paneH = 300;

        double currentX = node.getLayoutX();
        double currentY = node.getLayoutY();
        double currentW = node.getPrefWidth();
        double currentH = node.getPrefHeight();

        if (targetX >= 0) {
            if (targetX == 0.0) {
                currentX = 0;
            } else if (targetX == 1.0) {
                currentX = paneW - currentW;
            } else if (targetX == 0.5) {
                currentX = (paneW - currentW) / 2.0;
            }
            node.setLayoutX(currentX);
            currentLayout.setFieldPosition(field, currentX / paneW, currentY / paneH);
        }

        if (targetY >= 0) {
            if (targetY == 0.0) {
                currentY = 0;
            } else if (targetY == 1.0) {
                currentY = paneH - currentH;
            } else if (targetY == 0.5) {
                currentY = (paneH - currentH) / 2.0;
            }
            node.setLayoutY(currentY);
            currentLayout.setFieldPosition(field, currentX / paneW, currentY / paneH);
        }

        refreshPreview();
        persistCurrentLayoutIfPossible();
    }

    @FXML
    private void onResetFieldAdjustment() {
        LayoutField field = getSelectedField();
        if (field == null || currentLayout == null) {
            return;
        }

        BankTemplateLayout defaultLayout = new BankTemplateLayout(currentLayout.getWidthInches(), currentLayout.getHeightInches());
        FieldPosition defaultPos = defaultLayout.get(field);

        currentLayout.setFieldLayout(field, defaultPos.getXRatio(), defaultPos.getYRatio(), defaultPos.getWidthRatio(), defaultPos.getHeightRatio());

        if (cmbFontFamily != null) cmbFontFamily.setValue("Arial");
        if (fldFontSize != null) fldFontSize.setText("12");

        applySelectedFieldFont(field, "Arial", 12);
        loadAdjustmentFields(field);
        refreshPreview();
        persistCurrentLayoutIfPossible();
    }

    @FXML
    private void onApplyFieldAdjustment() {
        LayoutField field = getSelectedField();
        if (field == null || currentLayout == null) {
            return;
        }

        try {
            double leftMm = parsePositive(fldAdjustLeft.getText(), "X (mm)");
            double topMm = parsePositive(fldAdjustTop.getText(), "Y (mm)");

            double widthMm = currentLayout.getWidthInches() * 25.4;
            double heightMm = currentLayout.getHeightInches() * 25.4;

            double xRatio = leftMm / widthMm;
            double yRatio = topMm / heightMm;

            double widthRatio = -1;
            if (fldAdjustWidth != null && !fldAdjustWidth.getText().isBlank()) {
                double wMm = parsePositive(fldAdjustWidth.getText(), "Width (mm)");
                widthRatio = wMm / widthMm;
            }

            double heightRatio = -1;
            if (fldAdjustHeight != null && !fldAdjustHeight.getText().isBlank()) {
                double hMm = parsePositive(fldAdjustHeight.getText(), "Height (mm)");
                heightRatio = hMm / heightMm;
            }

            currentLayout.setFieldLayout(field, xRatio, yRatio, widthRatio, heightRatio);

            String fontFamily = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue() : "Arial";
            int fontSize = getSelectedFontSize();
            applySelectedFieldFont(field, fontFamily, fontSize);

            refreshPreview();
            persistCurrentLayoutIfPossible();
        } catch (Exception ex) {
            showAlert("Adjustment Error", ex.getMessage(), Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void onPresetSmall() { setPresetSize(40.0, 8.0); }
    @FXML
    private void onPresetMedium() { setPresetSize(80.0, 10.0); }
    @FXML
    private void onPresetLarge() { setPresetSize(120.0, 12.0); }
    @FXML
    private void onPresetFullWidth() {
        if (currentLayout == null) return;
        double chequeWmm = currentLayout.getWidthInches() * 25.4;
        setPresetSize(chequeWmm * 0.9, 10.0);
    }

    private void setPresetSize(double widthMm, double heightMm) {
        if (currentLayout == null) return;
        LayoutField field = getSelectedField();
        if (field == null) return;

        double widthInches = currentLayout.getWidthInches();
        double heightInches = currentLayout.getHeightInches();
        double widthRatio = widthMm / (widthInches * 25.4);
        double heightRatio = heightMm / (heightInches * 25.4);

        FieldPosition pos = currentLayout.get(field);
        currentLayout.setFieldLayout(field, pos.getXRatio(), pos.getYRatio(), widthRatio, heightRatio);

        refreshPreview();
        persistCurrentLayoutIfPossible();
    }

    @FXML
    private void onExportJson() {
        if (currentLayout == null) {
            showAlert("Export Error", "No layout loaded to export.", Alert.AlertType.WARNING);
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Cheque Layout JSON");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName((selectedBank != null ? selectedBank.getBankCode() : "layout") + "_template.json");
        File file = fileChooser.showSaveDialog(chequePreviewPane.getScene().getWindow());
        if (file != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, currentLayout);
                showAlert("Export Success", "Layout exported successfully to: " + file.getName(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export layout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void onImportJson() {
        if (currentLayout == null) {
            showAlert("Import Error", "Please select a bank template first before importing a layout.", Alert.AlertType.WARNING);
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Import Cheque Layout JSON");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(chequePreviewPane.getScene().getWindow());
        if (file != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                BankTemplateLayout imported = mapper.readValue(file, BankTemplateLayout.class);
                if (imported != null) {
                    currentLayout = imported;
                    currentLayout.ensureAllFields();
                    layoutPreviewPane();
                    refreshPreview();
                    persistCurrentLayoutIfPossible();
                    showAlert("Import Success", "Layout imported successfully from: " + file.getName(), Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                showAlert("Import Error", "Failed to import layout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void saveTemplateFieldsToApi(Long templateId) {
        if (templateId == null || templateId <= 0) {
            templateId = 1L;
        }

        List<Map<String, Object>> fieldsPayload = new ArrayList<>();
        String fontFamily = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue() : "Arial";
        int fontSize = 12;
        if (fldFontSize != null && !fldFontSize.getText().isBlank()) {
            try {
                fontSize = Integer.parseInt(fldFontSize.getText().trim());
            } catch (Exception ignored) {}
        }

        for (Map.Entry<LayoutField, StackPane> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            StackPane node = entry.getValue();

            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("templateId", templateId);
            fieldMap.put("fieldName", mapFieldName(field));
            fieldMap.put("xPosition", node.getLayoutX());
            fieldMap.put("yPosition", node.getLayoutY());
            fieldMap.put("fontSize", fontSize);
            fieldMap.put("fontFamily", fontFamily);

            fieldsPayload.add(fieldMap);
        }

        final Long targetTemplateId = templateId;
        new Thread(() -> {
            try {
                // Call POST /api/template/fields
                boolean success = bankService.saveTemplateFields(fieldsPayload);
                if (success) {
                    // Reload template fields from GET /api/template/fields/{templateId}
                    List<Map<String, Object>> reloadedFields = bankService.getTemplateFields(targetTemplateId);
                    Platform.runLater(() -> {
                        applyReloadedFields(reloadedFields);
                        System.out.println("Successfully reloaded template fields from REST API.");
                    });
                }
            } catch (Exception ex) {
                System.err.println("Failed to save/reload template fields from REST API: " + ex.getMessage());
            }
        }, "save-template-fields-api").start();
    }

    private String mapFieldName(LayoutField field) {
        return switch (field) {
            case PAYEE -> "name";
            case AMOUNT_NUMBER -> "amount";
            case AMOUNT_WORDS -> "amount_words";
            case DATE -> "date";
            case SIGNATURE -> "signature";
            case BANK_LOGO -> "logo";
            case MICR -> "micr";
        };
    }

    private void clearOldUI() {
        setSelectedField(null);
        if (lblActiveLayerName != null) {
            lblActiveLayerName.setText("None");
        }
        if (lblCoordinatesHUD != null) {
            lblCoordinatesHUD.setText("Select an element");
        }
        if (fldAdjustLeft != null) fldAdjustLeft.clear();
        if (fldAdjustTop != null) fldAdjustTop.clear();
        if (fldAdjustWidth != null) fldAdjustWidth.clear();
        if (fldAdjustHeight != null) fldAdjustHeight.clear();
        if (inspectorGrid != null) inspectorGrid.setDisable(true);
        if (alignmentPanel != null) alignmentPanel.setDisable(true);

        updateFieldHighlights();
    }

    public void loadNewTemplate(Long bankId, Bank bank) {
        if (bankId == null || bankId <= 0) {
            return;
        }

        // Check Map<BankId, Template> cache first
        if (bankTemplateMap.containsKey(bankId)) {
            currentLayout = bankTemplateMap.get(bankId).copy();
            layoutPreviewPane();
            refreshPreview();
        }

        new Thread(() -> {
            try {
                // Call API GET /api/template/{bankId}
                List<Map<String, Object>> templates = bankService.getTemplatesByBankId(bankId);
                Long templateId = bankId;
                if (!templates.isEmpty() && templates.get(0).get("id") instanceof Number) {
                    templateId = ((Number) templates.get(0).get("id")).longValue();
                }

                // Call API GET /api/template/fields/{templateId}
                List<Map<String, Object>> fields = bankService.getTemplateFields(templateId);

                final Long targetBankId = bankId;
                Platform.runLater(() -> {
                    applyReloadedFields(fields);
                    if (currentLayout != null) {
                        bankTemplateMap.put(targetBankId, currentLayout.copy());
                    }
                });
            } catch (Exception e) {
                System.err.println("Multi-bank template load warning: " + e.getMessage());
            }
        }, "load-new-template").start();
    }

    public void loadTemplateFromBackend(Long bankId) {
        loadNewTemplate(bankId, selectedBank);
    }

    private void applyReloadedFields(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty() || currentLayout == null) {
            return;
        }

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0) paneW = 720;
        if (paneH <= 0) paneH = 300;

        for (Map<String, Object> map : fields) {
            String name = (String) map.get("fieldName");
            Object xObj = map.get("xPosition");
            Object yObj = map.get("yPosition");
            Object fontFamilyObj = map.get("fontFamily");
            Object fontSizeObj = map.get("fontSize");

            if (name != null && xObj instanceof Number && yObj instanceof Number) {
                double x = ((Number) xObj).doubleValue();
                double y = ((Number) yObj).doubleValue();
                String fontFamily = fontFamilyObj instanceof String ? (String) fontFamilyObj : "Arial";
                int fontSize = fontSizeObj instanceof Number ? ((Number) fontSizeObj).intValue() : 12;

                LayoutField field = unmapFieldName(name);
                if (field != null) {
                    StackPane node = fieldNodes.get(field);
                    if (node != null) {
                        // Step 3: Render fields on Pane - Set layoutX and layoutY
                        node.setLayoutX(x);
                        node.setLayoutY(y);

                        // Step 4: Apply font settings
                        for (javafx.scene.Node child : node.getChildren()) {
                            if (child instanceof Label label) {
                                label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                                label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px;");
                            }
                        }

                        currentLayout.setFieldPosition(field, x / paneW, y / paneH);
                    }
                }
            }
        }
        refreshPreview();
    }

    private LayoutField unmapFieldName(String name) {
        return switch (name.toLowerCase()) {
            case "name", "payee" -> LayoutField.PAYEE;
            case "amount", "amount_number" -> LayoutField.AMOUNT_NUMBER;
            case "amount_words" -> LayoutField.AMOUNT_WORDS;
            case "date" -> LayoutField.DATE;
            case "signature" -> LayoutField.SIGNATURE;
            case "logo" -> LayoutField.BANK_LOGO;
            case "micr" -> LayoutField.MICR;
            default -> null;
        };
    }

    private void applySelectedFieldFont(LayoutField field, String fontFamily, int fontSize) {
        if (field == null) return;
        StackPane node = fieldNodes.get(field);
        if (node != null) {
            for (javafx.scene.Node child : node.getChildren()) {
                if (child instanceof Label label) {
                    label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                    label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px;");
                }
            }
        }
    }

    private int getSelectedFontSize() {
        if (fldFontSize != null && !fldFontSize.getText().isBlank()) {
            try {
                return Integer.parseInt(fldFontSize.getText().trim());
            } catch (Exception ignored) {}
        }
        return 12;
    }

    private static final class Delta {
        double x;
        double y;
    }
}

