package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.ChequeTemplate;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.service.BankService;
import com.chequeprint.util.AppState;
import com.chequeprint.util.BankTemplatePdfExporter;
import com.chequeprint.util.ChequePreviewEngine;
import com.chequeprint.util.ChequeSizeCodec;
import com.chequeprint.util.ChequeSizePreset;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.Session;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import com.chequeprint.service.PrintService;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chequeprint.service.ApiService;
import com.chequeprint.model.BankAccount;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.stage.Window;

public class BankTemplateController extends BankAccountController {

    private static final Logger LOGGER = Logger.getLogger(BankTemplateController.class.getName());
    private final ApiService apiService = new ApiService();
    @FXML
    private TableView<Bank> bankTable;
    @FXML
    private TableView<BankAccount> accountTable;
    @FXML
    private Button btnAddAccount;
    @FXML
    private Button btnEditAccountAction;
    @FXML
    private Button btnDeleteAccountAction;
    @FXML
    private VBox emptyState;
    @FXML
    private VBox loadingSpinner;
    @FXML
    private VBox previewEmptyState;
    @FXML
    private Pane previewPane;
    @FXML
    private VBox previewLoading;
    @FXML
    private Button btnEditTemplate;
    @FXML
    private ComboBox<com.chequeprint.model.ChequeTemplate> cmbAccountTemplates;
    @FXML
    private Label lblTemplateStatus;
    @FXML
    private Button btnSetAsDefault;
    @FXML
    private Button btnPreviewTemplate;

    private static final double PREVIEW_PPI = 90.0;
    private final Map<Long, BankTemplateLayout> bankTemplateMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean isProcessing = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    @FXML
    private ComboBox<BankAccount> cmbBankAccount;
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
    private Label lblTemplateMapping;
    @FXML
    private Label lblTemplateMappingDesigner;
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

    private final PrintService printService = new PrintService();
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
    private boolean initialized = false;
    private boolean loadingBankAccounts = false;
    private boolean accountTemplateListenerRegistered = false;
    private final Set<String> templateLoadsInFlight = ConcurrentHashMap.newKeySet();
    private final Set<Long> layoutLoadsInFlight = ConcurrentHashMap.newKeySet();

    // â”€â”€ Preview rendering optimisation fields â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /** 16 ms debounce timer: coalesces rapid refreshPreview() calls to one frame. */
    private javafx.animation.PauseTransition previewDebounceTimer;
    /** 50 ms debounce timer for viewport resize â€” avoids double-fire from width+height listeners. */
    private javafx.animation.PauseTransition layoutDebounceTimer;
    /**
     * When true, the AppState selectedTemplateProperty listener skips calling
     * refreshPreview(). Set during drag / resize so the in-loop AppState update
     * does not trigger a second full refresh.
     */
    private boolean suppressPreviewListener = false;
    /**
     * True while a field node is being dragged or resized.
     * Used to skip expensive inspector / highlight updates inside refreshPreview().
     */
    private boolean isDragging = false;
    /**
     * Last known pixel positions for each field node, keyed by LayoutField ordinal.
     * Index layout: [0]=layoutX, [1]=layoutY, [2]=prefWidth, [3]=prefHeight.
     * Allows refreshPreview() to skip setLayoutX/Y when the value has not changed.
     */
    private final Map<LayoutField, double[]> lastPositions = new EnumMap<>(LayoutField.class);
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static final class Delta {
        private double x;
        private double y;
    }

    // â”€â”€ Debounce helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Schedules refreshPreview() to run after a 16 ms delay (~one render frame).
     * Multiple calls within the same frame are coalesced into a single refresh.
     * Use this on the hot drag/resize path instead of calling refreshPreview() directly.
     */
    private void schedulePreviewRefresh() {
        if (previewDebounceTimer == null) {
            previewDebounceTimer = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(16));
            previewDebounceTimer.setOnFinished(e -> refreshPreview());
        }
        previewDebounceTimer.playFromStart();
    }

    /**
     * Schedules layoutPreviewPane() after a 50 ms delay.
     * Prevents the width + height property listeners from firing two independent
     * layout passes on every window resize event.
     */
    private void scheduledLayoutPreviewPane() {
        if (layoutDebounceTimer == null) {
            layoutDebounceTimer = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(50));
            layoutDebounceTimer.setOnFinished(e -> layoutPreviewPane());
        }
        layoutDebounceTimer.playFromStart();
    }
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void setLayerButtonSelected(Button layerButton, boolean selected) {
        if (layerButton == null) {
            return;
        }

        if (selected) {
            layerButton.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #1d4ed8; -fx-border-width: 1px;");
        } else {
            layerButton.setStyle(
                    "-fx-background-color: #f8fafc; -fx-text-fill: #0f172a; -fx-font-weight: normal; -fx-border-color: #cbd5e1; -fx-border-width: 1px;");
        }
    }

    @FXML
    private void onAddAccount() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/bank_account_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            BankAccountDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            if (accountTable != null && accountTable.getScene() != null) {
                stage.initOwner(accountTable.getScene().getWindow());
            }
            stage.setTitle("Add Bank Account");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            com.chequeprint.util.ThemeManager.applySavedTheme(scene);

            // Drag support
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            // Background blur/dim
            javafx.scene.Parent ownerRoot = accountTable != null && accountTable.getScene() != null
                    ? accountTable.getScene().getRoot()
                    : null;
            javafx.scene.effect.Effect oldEffect = ownerRoot != null ? ownerRoot.getEffect() : null;
            if (ownerRoot != null) {
                ownerRoot.setEffect(new javafx.scene.effect.BoxBlur(6, 6, 3));
            }
            stage.setOnHiding(e -> {
                if (ownerRoot != null) {
                    ownerRoot.setEffect(oldEffect);
                }
            });

            controller.setOnSaveCallback(account -> {
                // Check if account number exists
                BankAccount existingAcc = null;
                if (account.getAccountNumber() != null) {
                    for (BankAccount acc : accountData) {
                        if (acc.getAccountNumber() != null
                                && acc.getAccountNumber().equalsIgnoreCase(account.getAccountNumber().trim())) {
                            existingAcc = acc;
                            break;
                        }
                    }
                }

                final BankAccount match = existingAcc;
                final boolean exists = (match != null);

                Task<BankAccount> saveTask = new Task<>() {
                    @Override
                    protected BankAccount call() throws Exception {
                        if (exists && match.getId() != null) {
                            return apiService.updateBankAccount(match.getId(), account);
                        }
                        return apiService.saveBankAccount(account);
                    }
                };
                saveTask.setOnSucceeded(ev -> {
                    BankAccount saved = saveTask.getValue();

                    if (exists && match != null) {
                        int idx = accountData.indexOf(match);
                        if (idx >= 0) {
                            accountData.set(idx, saved);
                        } else {
                            accountData.add(saved);
                        }
                    } else {
                        accountData.add(saved);
                    }

                    // Reload fresh bank list and account list from REST API
                    loadBankAccounts();
                    loadData();

                    if (accountTable != null) {
                        accountTable.refresh();
                    }
                    if (emptyState != null) {
                        emptyState.setVisible(accountData.isEmpty());
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(exists ? "Account Exists" : "Success");
                    alert.setHeaderText(null);
                    alert.setContentText(exists ? "Account already exists! Details updated successfully."
                            : "Bank account saved successfully to database!");
                    alert.showAndWait();
                });
                saveTask.setOnFailed(ev -> {
                    Throwable ex = saveTask.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to Save Account");
                    alert.setContentText(ex != null ? ex.getMessage() : "Unknown error");
                    alert.showAndWait();
                });
                new Thread(saveTask).start();
            });

            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onEditAccountAction() {
        if (accountTable == null || accountTable.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        BankAccount selected = accountTable.getSelectionModel().getSelectedItem();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/bank_account_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            BankAccountDialogController controller = loader.getController();
            controller.initData(selected);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            if (accountTable.getScene() != null) {
                stage.initOwner(accountTable.getScene().getWindow());
            }
            stage.setTitle("Edit Bank Account");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            com.chequeprint.util.ThemeManager.applySavedTheme(scene);

            // Drag support
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            // Blur effect
            javafx.scene.Parent ownerRoot = accountTable.getScene() != null ? accountTable.getScene().getRoot() : null;
            javafx.scene.effect.Effect oldEffect = ownerRoot != null ? ownerRoot.getEffect() : null;
            if (ownerRoot != null) {
                ownerRoot.setEffect(new javafx.scene.effect.BoxBlur(6, 6, 3));
            }
            stage.setOnHiding(e -> {
                if (ownerRoot != null) {
                    ownerRoot.setEffect(oldEffect);
                }
            });

            controller.setOnSaveCallback(updatedAccount -> {
                Task<BankAccount> updateTask = new Task<>() {
                    @Override
                    protected BankAccount call() throws Exception {
                        if (selected.getId() != null) {
                            return apiService.updateBankAccount(selected.getId(), updatedAccount);
                        }
                        return apiService.saveBankAccount(updatedAccount);
                    }
                };
                updateTask.setOnSucceeded(ev -> {
                    BankAccount result = updateTask.getValue();
                    int idx = accountData.indexOf(selected);
                    if (idx >= 0) {
                        accountData.set(idx, result);
                    }
                    loadBankAccounts();
                    loadData();
                    if (accountTable != null) {
                        accountTable.refresh();
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Bank account updated successfully!");
                    alert.showAndWait();
                });
                updateTask.setOnFailed(ev -> {
                    Throwable ex = updateTask.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to Update Account");
                    alert.setContentText(ex != null ? ex.getMessage() : "Unknown error");
                    alert.showAndWait();
                });
                new Thread(updateTask).start();
            });

            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onDeleteAccountAction() {
        if (accountTable == null || accountTable.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        BankAccount selected = accountTable.getSelectionModel().getSelectedItem();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Bank Account");
        confirm.setHeaderText("Are you sure you want to delete this bank account?");
        confirm.setContentText(selected.getBankName() + " (" + selected.getAccountNumber() + ")");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (selected.getId() != null) {
                            apiService.deleteBankAccount(selected.getId());
                        }
                        return null;
                    }
                };
                deleteTask.setOnSucceeded(ev -> {
                    accountData.remove(selected);
                    loadBankAccounts();
                    loadData();
                    if (accountTable != null) {
                        accountTable.refresh();
                    }
                    if (emptyState != null) {
                        emptyState.setVisible(accountData.isEmpty());
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Deleted");
                    alert.setHeaderText(null);
                    alert.setContentText("Bank account deleted successfully.");
                    alert.showAndWait();
                });
                deleteTask.setOnFailed(ev -> {
                    Throwable ex = deleteTask.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to Delete Account");
                    alert.setContentText(ex != null ? ex.getMessage() : "Unknown error");
                    alert.showAndWait();
                });
                new Thread(deleteTask).start();
            }
        });
    }

    @FXML
    private void onEditTemplate() {
        if (accountTable != null && accountTable.getSelectionModel().getSelectedItem() != null) {
            BankAccount selected = accountTable.getSelectionModel().getSelectedItem();
            if (cmbBankAccount != null) {
                cmbBankAccount.setValue(selected);
            }
            if (selected.getId() != null) {
                Long bankId = selected.getId().longValue();
                Session.setSelectedBankId(bankId);
                loadTemplateFromBackend(bankId);
            }
        }
        // Switch to the template designer tab
        if (cmbBankAccount != null && cmbBankAccount.getScene() != null) {
            javafx.scene.Node tabPaneNode = cmbBankAccount.getScene().lookup(".tab-pane");
            if (tabPaneNode instanceof javafx.scene.control.TabPane tabPane) {
                if (tabPane.getTabs().size() > 1) {
                    tabPane.getSelectionModel().select(1);
                }
            }
        }
    }

    @FXML
    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        setupForm();
        setupPreview();
        setupAdjustmentPanel();
        setupAccountTableListener();
        loadLayouts();
        loadData();
        loadBankAccounts();
        clearForm(); // Ensures default layout coordinates are applied at startup
        FxUtils.animateIn(previewViewport, 0);

        AppState.getInstance().selectedTemplateProperty().addListener((obs, oldTemplate, latestTemplate) -> {
            // Guard: skip when the update originates from our own drag/resize loop
            // to prevent a double-refresh on every mouse-drag event.
            if (!suppressPreviewListener && latestTemplate != null) {
                this.currentLayout = latestTemplate;
                refreshPreview();
            }
        });
    }

    private com.chequeprint.model.ChequeTemplate currentTemplate = new com.chequeprint.model.ChequeTemplate();

    public com.chequeprint.model.ChequeTemplate getCurrentTemplate() {
        return currentTemplate;
    }

    public void clearPreviewPane() {
        if (previewPane != null) {
            previewPane.getChildren().clear();
        }
    }

    public void setCurrentTemplate(com.chequeprint.model.ChequeTemplate template) {
        this.currentTemplate = template != null ? template : new com.chequeprint.model.ChequeTemplate();
        renderPreview(this.currentTemplate);
    }

    public void updateFieldPosition(String fieldName, double x, double y, double fontSize, boolean visible) {
        if (currentTemplate == null) {
            currentTemplate = new com.chequeprint.model.ChequeTemplate();
        }

        com.chequeprint.model.ChequeTemplate.FieldConfig cfg = switch (fieldName != null
                ? fieldName.toLowerCase().trim()
                : "") {
            case "date", "datefield" -> currentTemplate.getDateField();
            case "payee", "payeefield" -> currentTemplate.getPayeeField();
            case "amountwords", "words" -> currentTemplate.getAmountWordsField();
            case "amountnum", "figures" -> currentTemplate.getAmountNumField();
            case "acpayee" -> currentTemplate.getAcPayeeField();
            case "bearer" -> currentTemplate.getBearerField();
            case "signature" -> currentTemplate.getSignatureField();
            case "micr" -> currentTemplate.getMicrField();
            default -> null;
        };

        if (cfg != null) {
            cfg.setX(x);
            cfg.setY(y);
            if (fontSize > 0)
                cfg.setFontSize(fontSize);
            cfg.setVisible(visible);

            // Re-render live preview instantly in real-time
            setCurrentTemplate(currentTemplate);
        }
    }

    private void updateTemplateMappingLabel(BankAccount account) {
        if (account == null) {
            if (lblTemplateMapping != null)
                lblTemplateMapping.setText("Select an account to view template mapping");
            if (lblTemplateMappingDesigner != null)
                lblTemplateMappingDesigner.setText("Select a Bank Account");
            return;
        }

        String bankName = account.getBankName() != null && !account.getBankName().isBlank() ? account.getBankName()
                : "Bank Account";
        String accNo = account.getAccountNumber() != null ? account.getAccountNumber().trim() : "";
        String last4 = accNo.length() >= 4 ? accNo.substring(accNo.length() - 4) : accNo;
        String labelText = bankName + (last4.isEmpty() ? "" : " (â€¢â€¢â€¢ " + last4 + ")");

        if (lblTemplateMapping != null) {
            lblTemplateMapping.setText("Template for: " + labelText);
        }
        if (lblTemplateMappingDesigner != null) {
            lblTemplateMappingDesigner.setText(labelText);
        }
    }

    private void setupAccountTableListener() {
        if (accountTable != null) {
            if (accountTable.getColumns().size() >= 5) {
                accountTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("bankName"));
                accountTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
                accountTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("accountHolderName"));
                accountTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("ifscCode"));
                accountTable.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("branchName"));
            }
            accountTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdatingForm)
                    return;
                try {
                    isUpdatingForm = true;
                    boolean hasSelection = (newVal != null);
                    if (btnEditAccountAction != null)
                        btnEditAccountAction.setDisable(!hasSelection);
                    if (btnDeleteAccountAction != null)
                        btnDeleteAccountAction.setDisable(!hasSelection);
                    updateTemplateMappingLabel(newVal);

                    if (newVal == null) {
                        if (previewEmptyState != null)
                            previewEmptyState.setVisible(true);
                        if (previewPane != null) {
                            previewPane.setVisible(false);
                            previewPane.getChildren().clear();
                        }
                        if (btnEditTemplate != null)
                            btnEditTemplate.setDisable(true);
                    } else {
                        if (previewEmptyState != null)
                            previewEmptyState.setVisible(false);
                        if (previewPane != null)
                            previewPane.setVisible(true);
                        if (btnEditTemplate != null)
                            btnEditTemplate.setDisable(false);

                        if (cmbBankAccount != null && cmbBankAccount.getValue() != newVal) {
                            cmbBankAccount.setValue(newVal);
                        }
                        if (newVal.getId() != null) {
                            Long bankId = newVal.getId().longValue();
                            Session.setSelectedBankId(bankId);
                            loadTemplateFromBackend(bankId);
                        } else {
                            setCurrentTemplate(new com.chequeprint.model.ChequeTemplate());
                        }
                        loadTemplatesForSelectedAccount(newVal);
                    }
                } finally {
                    isUpdatingForm = false;
                }
            });
        }
    }

    private void loadTemplatesForSelectedAccount(BankAccount account) {
        if (cmbAccountTemplates == null)
            return;
        if (account == null || account.getId() == null) {
            cmbAccountTemplates.getItems().clear();
            if (btnSetAsDefault != null)
                btnSetAsDefault.setDisable(true);
            if (btnPreviewTemplate != null)
                btnPreviewTemplate.setDisable(true);
            if (lblTemplateStatus != null)
                lblTemplateStatus.setText("NO SELECTION");
            return;
        }

        new Thread(() -> {
            try {
                Long accountId = account.getId().longValue();
                List<ChequeTemplate> templates = new ArrayList<>();
                try {
                    templates = apiService.getTemplatesByAccountId(accountId);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Unable to load account templates for accountId " + accountId, ex);
                }

                if (templates.isEmpty()) {
                    ChequeTemplate defaultTpl = new ChequeTemplate();
                    defaultTpl.setId(account.getTemplateId() != null ? account.getTemplateId() : 1L);
                    defaultTpl.setTemplateName(account.getBankName() + " Standard CTS-2010");
                    templates.add(defaultTpl);
                }

                final List<ChequeTemplate> finalTemplates = templates;
                Platform.runLater(() -> {
                    cmbAccountTemplates.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(ChequeTemplate t) {
                            if (t == null)
                                return "";
                            boolean isDef = account.getTemplateId() != null
                                    && account.getTemplateId().equals(t.getId());
                            return (isDef ? "â­ [DEFAULT] " : "ðŸ“„ ")
                                    + (t.getTemplateName() != null ? t.getTemplateName() : "Template #" + t.getId());
                        }

                        @Override
                        public ChequeTemplate fromString(String string) {
                            return null;
                        }
                    });

                    cmbAccountTemplates.setItems(FXCollections.observableArrayList(finalTemplates));

                    ChequeTemplate defaultItem = finalTemplates.get(0);
                    for (ChequeTemplate t : finalTemplates) {
                        if (account.getTemplateId() != null && account.getTemplateId().equals(t.getId())) {
                            defaultItem = t;
                            break;
                        }
                    }
                    cmbAccountTemplates.setValue(defaultItem);

                    if (btnSetAsDefault != null)
                        btnSetAsDefault.setDisable(false);
                    if (btnPreviewTemplate != null)
                        btnPreviewTemplate.setDisable(false);

                    updateTemplateStatusBadge(account, defaultItem);

                    if (!accountTemplateListenerRegistered) {
                        accountTemplateListenerRegistered = true;
                        cmbAccountTemplates.valueProperty().addListener((obs, oldTpl, newTpl) -> {
                            BankAccount selectedAccount = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
                            if (newTpl != null) {
                                updateTemplateStatusBadge(selectedAccount, newTpl);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "load-acc-templates").start();
    }

    private void updateTemplateStatusBadge(BankAccount account, ChequeTemplate selectedTpl) {
        if (lblTemplateStatus == null)
            return;
        boolean isDefault = (account != null && account.getTemplateId() != null && selectedTpl != null
                && account.getTemplateId().equals(selectedTpl.getId()));
        if (isDefault) {
            lblTemplateStatus.setText("â­ ACTIVE DEFAULT");
            lblTemplateStatus.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
        } else {
            lblTemplateStatus.setText("OPTIONAL TEMPLATE");
            lblTemplateStatus.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
        }
    }

    @FXML
    private void onSetAsDefaultTemplate() {
        BankAccount selectedAcc = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;
        ChequeTemplate selectedTpl = cmbAccountTemplates != null ? cmbAccountTemplates.getValue()
                : null;

        if (selectedAcc == null || selectedTpl == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a Bank Account and Template first.",
                    ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        try {
            Long accId = selectedAcc.getId().longValue();
            Long tplId = selectedTpl.getId();

            apiService.setDefaultTemplate(accId, tplId);

            selectedAcc.setTemplateId(tplId);
            AppState.getInstance().setSelectedBankAccount(selectedAcc);

            updateTemplateStatusBadge(selectedAcc, selectedTpl);
            cmbAccountTemplates.setItems(FXCollections.observableArrayList(cmbAccountTemplates.getItems()));
            cmbAccountTemplates.setValue(selectedTpl);

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION, "Template '" + selectedTpl.getTemplateName()
                            + "' is now the active DEFAULT template for " + selectedAcc.getBankName() + "!",
                    ButtonType.OK);
            alert.setTitle("Default Template Updated");
            alert.setHeaderText(null);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update default template: " + e.getMessage(),
                    ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void onPreviewSelectedTemplate() {
        com.chequeprint.model.ChequeTemplate selectedTpl = cmbAccountTemplates != null ? cmbAccountTemplates.getValue()
                : null;
        BankAccount selectedAcc = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;

        if (selectedTpl == null && selectedAcc != null && selectedAcc.getId() != null) {
            loadTemplateFromBackend(selectedAcc.getId().longValue());
            return;
        }
        if (selectedTpl != null) {
            setCurrentTemplate(selectedTpl);
            if (previewEmptyState != null)
                previewEmptyState.setVisible(false);
            if (previewPane != null)
                previewPane.setVisible(true);
        }
    }

    private final Map<String, com.chequeprint.model.ChequeTemplate> templateCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<Long> failedBankIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private Long currentlyLoadedBankId = null;
    private javafx.animation.PauseTransition templateDebounceTimer = null;

    private void loadTemplateFromBackend(Long bankId) {
        if (bankId == null) {
            currentlyLoadedBankId = null;
            setCurrentTemplate(new com.chequeprint.model.ChequeTemplate());
            return;
        }

        // 1. Check: If same bankId already loaded â†’ DO NOT call API again
        if (java.util.Objects.equals(currentlyLoadedBankId, bankId) && currentTemplate != null) {
            LOGGER.info("[BankController] Bank ID " + bankId + " is already loaded. Skipping API call.");
            return;
        }

        String cacheKey = String.valueOf(bankId);

        // 2. Check in-memory cache
        if (templateCache.containsKey(cacheKey)) {
            LOGGER.info("[BankController] Serving cached template for Bank ID " + bankId);
            currentlyLoadedBankId = bankId;
            setCurrentTemplate(templateCache.get(cacheKey));
            return;
        }

        // 3. Condition: Prevent repeated calls if previous request failed
        if (failedBankIds.contains(bankId)) {
            LOGGER.warning("[BankController] Previous request for Bank ID " + bankId
                    + " failed. Serving cached fallback template without calling API.");
            com.chequeprint.model.ChequeTemplate fallback = new com.chequeprint.model.ChequeTemplate(bankId,
                    "Default Bank Template");
            templateCache.put(cacheKey, fallback);
            currentlyLoadedBankId = bankId;
            setCurrentTemplate(fallback);
            return;
        }

        // 4. In-flight request deduplication
        if (!templateLoadsInFlight.add(cacheKey)) {
            return;
        }

        // 5. Add Debounce (300ms delay)
        if (templateDebounceTimer != null) {
            templateDebounceTimer.stop();
        }

        templateDebounceTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        templateDebounceTimer.setOnFinished(evt -> executeTemplateFetchTask(bankId, cacheKey));
        templateDebounceTimer.play();
    }

    public void renderPreview(com.chequeprint.model.ChequeTemplate template) {
        if (previewPane == null)
            return;
        com.chequeprint.util.ChequeRenderEngine.initializePreviewElements(previewPane);
        com.chequeprint.util.ChequeRenderEngine.renderCheque(previewPane, AppState.getInstance().getCurrentCheque(),
                selectedBank, AppState.getInstance().getSelectedTemplate());
    }

    private void setupForm() {
        if (cmbBankAccount != null) {
            cmbBankAccount.setItems(accountData);
            cmbBankAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdatingForm)
                    return;
                try {
                    isUpdatingForm = true;
                    updateTemplateMappingLabel(newVal);
                    if (newVal != null && newVal.getId() != null) {
                        Long bankId = newVal.getId().longValue();
                        Session.setSelectedBankId(bankId);
                        AppState.getInstance().setSelectedBankAccount(newVal);

                        if (fldBankCode != null && newVal.getBankName() != null) {
                            fldBankCode.setText(newVal.getBankName());
                        }
                        if (accountTable != null && accountTable.getSelectionModel().getSelectedItem() != newVal) {
                            accountTable.getSelectionModel().select(newVal);
                        }
                        loadTemplateFromBackend(bankId);
                    }
                } finally {
                    isUpdatingForm = false;
                }
            });
        }
        if (cmbFontFamily != null) {
            cmbFontFamily.setItems(FXCollections.observableArrayList("Arial", "Courier New", "Consolas",
                    "Times New Roman", "Verdana", "Tahoma"));
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
                        String family = cmbFontFamily != null && cmbFontFamily.getValue() != null
                                ? cmbFontFamily.getValue()
                                : "Arial";
                        applySelectedFieldFont(getSelectedField(), family, size);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
        if (btnDelete != null)
            btnDelete.setDisable(true);

        if (cmbChequeSize != null) {
            cmbChequeSize.setItems(FXCollections.observableArrayList(ChequeSizePreset.values()));
            cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        }
        if (chkMicr != null)
            chkMicr.setSelected(true);

        if (fldCustomWidth != null)
            fldCustomWidth.setDisable(true);
        if (fldCustomHeight != null)
            fldCustomHeight.setDisable(true);

        if (cmbChequeSizeUnit != null) {
            cmbChequeSizeUnit.setItems(FXCollections.observableArrayList(
                    "Inches (in)", "Millimeters (mm)", "Centimeters (cm)", "Pixels (300 DPI)", "Pixels (72 DPI)"));
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
                if (lblCustomWidth != null)
                    lblCustomWidth.setText("Custom Width " + unitSuffix);
                if (lblCustomHeight != null)
                    lblCustomHeight.setText("Custom Height " + unitSuffix);

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

        if (cmbChequeSize != null) {
            cmbChequeSize.valueProperty().addListener((obs, old, preset) -> {
                boolean custom = preset == ChequeSizePreset.CUSTOM;
                if (fldCustomWidth != null)
                    fldCustomWidth.setDisable(!custom);
                if (fldCustomHeight != null)
                    fldCustomHeight.setDisable(!custom);
                if (cmbChequeSizeUnit != null) {
                    cmbChequeSizeUnit.setDisable(!custom);
                    if (!custom) {
                        cmbChequeSizeUnit.setValue("Inches (in)");
                        currentUnit = "Inches (in)";
                        if (lblCustomWidth != null)
                            lblCustomWidth.setText("Custom Width (in)");
                        if (lblCustomHeight != null)
                            lblCustomHeight.setText("Custom Height (in)");
                    }
                }
                if (!custom) {
                    if (fldCustomWidth != null)
                        fldCustomWidth.clear();
                    if (fldCustomHeight != null)
                        fldCustomHeight.clear();
                }
                refreshLayoutForSizeChange();
            });
        }

        if (fldCustomWidth != null)
            fldCustomWidth.textProperty().addListener((obs, o, n) -> refreshLayoutForSizeChange());
        if (fldCustomHeight != null)
            fldCustomHeight.textProperty().addListener((obs, o, n) -> refreshLayoutForSizeChange());

        if (chkMicr != null)
            chkMicr.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        if (chkSnapGrid != null) {
            chkSnapGrid.setSelected(true);
            chkSnapGrid.selectedProperty().addListener((obs, o, n) -> updateGridOverlay());
        }
    }

    private void setupPreview() {
        if (chequePreviewPane != null) {
            chequePreviewPane.setStyle(
                    "-fx-background-color:white; -fx-border-color:#94a3b8; -fx-border-width:1; -fx-background-radius:10; -fx-border-radius:10;");
        }
        if (previewViewport != null) {
            previewViewport.widthProperty().addListener((obs, old, v) -> scheduledLayoutPreviewPane());
            previewViewport.heightProperty().addListener((obs, old, v) -> scheduledLayoutPreviewPane());
        }
        if (previewPane != null) {
            com.chequeprint.util.ChequeRenderEngine.initializePreviewElements(previewPane);
        }
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
            showAlert("Load Error", ex != null ? ex.getMessage() : "Failed to load banks", Alert.AlertType.ERROR);
        });

        Thread thread = new Thread(task, "load-banks-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void executeTemplateFetchTask(Long bankId, String cacheKey) {
        if (previewLoading != null) {
            previewLoading.setVisible(true);
            previewLoading.setManaged(true);
        }

        Task<ChequeTemplate> task = new Task<>() {
            @Override
            protected ChequeTemplate call() throws Exception {
                return apiService.findChequeTemplateByBankId(bankId)
                        .orElseGet(() -> new ChequeTemplate(bankId, "Default Bank Template"));
            }
        };

        task.setOnSucceeded(e -> {
            templateLoadsInFlight.remove(cacheKey);
            if (previewLoading != null) {
                previewLoading.setVisible(false);
                previewLoading.setManaged(false);
            }
            ChequeTemplate template = task.getValue();
            if (template.getId() != null) {
                BankAccount sel = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    sel.setTemplateId(template.getId());
                }
            }
            currentlyLoadedBankId = bankId;
            templateCache.put(cacheKey, template);
            setCurrentTemplate(template);
        });

        task.setOnFailed(e -> {
            templateLoadsInFlight.remove(cacheKey);
            failedBankIds.add(bankId); // Track failure to prevent continuous retry hammering
            if (previewLoading != null) {
                previewLoading.setVisible(false);
                previewLoading.setManaged(false);
            }
            LOGGER.severe("Failed to fetch template for bankId " + bankId + ": " + task.getException().getMessage());
            com.chequeprint.model.ChequeTemplate defaultTemplate = new com.chequeprint.model.ChequeTemplate(bankId,
                    "Default Bank Template");
            templateCache.put(cacheKey, defaultTemplate);
            currentlyLoadedBankId = bankId;
            setCurrentTemplate(defaultTemplate);
        });

        new Thread(task, "fetch-template-bank-" + bankId).start();
    }

    @FXML
    private void onSaveTemplate() {
        if (!isProcessing.compareAndSet(false, true)) {
            LOGGER.warning("Duplicate save click suppressed â€” template save in progress.");
            return;
        }

        if (currentTemplate == null) {
            isProcessing.set(false);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Save Warning");
            alert.setHeaderText(null);
            alert.setContentText("No cheque template selected to save.");
            alert.showAndWait();
            return;
        }

        BankAccount selectedAcc = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;
        if (selectedAcc != null && selectedAcc.getId() != null) {
            currentTemplate.setBankId(selectedAcc.getId().longValue());
            currentTemplate.setTemplateName(selectedAcc.getBankName() + " Template");
        }

        setLoading(true);

        Task<ChequeTemplate> saveTask = new Task<>() {
            @Override
            protected ChequeTemplate call() throws Exception {
                currentTemplate.updateConfigJson();
                return apiService.saveChequeTemplate(currentTemplate);
            }
        };

        saveTask.setOnSucceeded(e -> {
            setLoading(false);
            isProcessing.set(false);

            ChequeTemplate saved = saveTask.getValue();
            if (saved.getBankId() != null) {
                templateCache.remove(String.valueOf(saved.getBankId()));
                loadTemplateFromBackend(saved.getBankId());
            } else {
                setCurrentTemplate(saved);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Template Saved");
            alert.setHeaderText("Success!");
            alert.setContentText("Cheque template updated successfully in database.");
            alert.showAndWait();
        });

        saveTask.setOnFailed(e -> {
            setLoading(false);
            isProcessing.set(false);

            Throwable ex = saveTask.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to Save Cheque Template");
            alert.setContentText(ex != null ? ex.getMessage() : "Unable to connect to Spring Boot REST server.");
            alert.showAndWait();
        });

        new Thread(saveTask, "save-template-async").start();
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
        new Thread(() -> {
            try {
                Map<String, BankTemplateLayout> loaded = bankService.loadAllLayouts();
                Platform.runLater(() -> {
                    layoutByBankCode.clear();
                    if (loaded != null) {
                        layoutByBankCode.putAll(loaded);
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load layout cache async: " + ex.getMessage());
            }
        }, "load-layouts-async").start();
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            if (cmbBankAccount != null && cmbBankAccount.getScene() != null
                    && cmbBankAccount.getScene().getRoot() != null) {
                cmbBankAccount.getScene().getRoot().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
            } else if (fldBankCode != null && fldBankCode.getScene() != null
                    && fldBankCode.getScene().getRoot() != null) {
                fldBankCode.getScene().getRoot().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
            }
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(loading);
                loadingSpinner.setManaged(loading);
            }
            if (previewLoading != null) {
                previewLoading.setVisible(loading);
                previewLoading.setManaged(loading);
            }
            if (btnSave != null)
                btnSave.setDisable(loading);
            if (btnDelete != null)
                btnDelete.setDisable(loading || selectedBank == null);
            if (btnClear != null)
                btnClear.setDisable(loading);
            if (btnNewBank != null)
                btnNewBank.setDisable(loading);
            if (btnAddAccount != null)
                btnAddAccount.setDisable(loading);
            if (btnEditAccountAction != null)
                btnEditAccountAction.setDisable(loading);
            if (btnDeleteAccountAction != null)
                btnDeleteAccountAction.setDisable(loading);
            if (btnSetAsDefault != null)
                btnSetAsDefault.setDisable(loading);
            if (btnEditTemplate != null)
                btnEditTemplate.setDisable(loading);
            if (btnPreviewTemplate != null)
                btnPreviewTemplate.setDisable(loading);
            if (bankTable != null)
                bankTable.setDisable(loading);
            if (accountTable != null)
                accountTable.setDisable(loading);
            if (cmbBankAccount != null)
                cmbBankAccount.setDisable(loading);
        });
    }

    private void loadBankAccounts() {
        if (loadingBankAccounts) {
            return;
        }
        loadingBankAccounts = true;
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
            BankAccount selectedBeforeReload = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
            accountData.setAll(accounts);
            if (accountTable != null) {
                accountTable.setItems(accountData);
                accountTable.refresh();
            }
            if (cmbBankAccount != null) {
                cmbBankAccount.setItems(accountData);
                Long currentGlobalId = Session.getSelectedBankId();
                BankAccount targetAcc = null;
                if (currentGlobalId != null) {
                    for (BankAccount acc : accounts) {
                        if (acc.getId() != null && acc.getId().longValue() == currentGlobalId) {
                            targetAcc = acc;
                            break;
                        }
                    }
                }
                if (targetAcc == null && selectedBeforeReload != null && selectedBeforeReload.getId() != null) {
                    for (BankAccount acc : accounts) {
                        if (acc.getId() != null && acc.getId().equals(selectedBeforeReload.getId())) {
                            targetAcc = acc;
                            break;
                        }
                    }
                }
                if (targetAcc != null) {
                    if (cmbBankAccount.getValue() == null
                            || !targetAcc.getId().equals(cmbBankAccount.getValue().getId())) {
                        cmbBankAccount.setValue(targetAcc);
                    }
                    AppState.getInstance().setSelectedBankAccount(targetAcc);
                    if (targetAcc.getId() != null) {
                        loadNewTemplate(targetAcc.getId().longValue(),
                                new Bank(targetAcc.getBankName(), targetAcc.getBankName(), "STANDARD", true));
                    }
                }
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
            loadingBankAccounts = false;
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (loadingSpinner != null) {
                loadingSpinner.setVisible(false);
                loadingSpinner.setManaged(false);
            }
            showAlert("API Error", "Failed to load bank accounts: " + ex.getMessage(), Alert.AlertType.ERROR);
            loadingBankAccounts = false;
        });

        Thread thread = new Thread(task, "load-accounts-task");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onSave() {
        BankAccount selectedAcc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        if (selectedAcc == null || selectedAcc.getId() == null) {
            showAlert("No Bank Account Selected", "Please choose a bank account before saving the template layout.",
                    Alert.AlertType.WARNING);
            return;
        }

        if (currentLayout == null) {
            showAlert("Save Error", "No cheque template layout is loaded to save.", Alert.AlertType.WARNING);
            return;
        }

        saveTemplateFieldsToApiInternal(selectedAcc.getId().longValue(), true);
    }

    @FXML
    private void onExportPdf() {
        if (currentLayout == null) {
            showAlert("Preview", "No template layout available to export.", Alert.AlertType.WARNING);
            return;
        }

        Bank bank = selectedBank != null ? selectedBank : buildDraftBank();
        if (bank.getBankName() == null || bank.getBankName().isBlank() || bank.getBankCode() == null
                || bank.getBankCode().isBlank()) {
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

    private void selectLayer(LayoutField field) {
        setSelectedField(field);
    }

    @FXML
    private void onSelectLayerDate() {
        selectLayer(LayoutField.DATE);
    }

    @FXML
    private void onSelectLayerPayee() {
        selectLayer(LayoutField.PAYEE);
    }

    @FXML
    private void onSelectLayerAmountWords() {
        selectLayer(LayoutField.AMOUNT_WORDS);
    }

    @FXML
    private void onSelectLayerAmountNumber() {
        selectLayer(LayoutField.AMOUNT_NUMBER);
    }

    @FXML
    private void onSelectLayerSignature() {
        selectLayer(LayoutField.SIGNATURE);
    }

    @FXML
    private void onSelectLayerBankLogo() {
        selectLayer(LayoutField.BANK_LOGO);
    }

    @FXML
    private void onSelectLayerMicr() {
        selectLayer(LayoutField.MICR);
    }

    private void populateForm(Bank bank) {
        if (bank == null) {
            clearOldUI();
            return;
        }

        // 1. Clear old UI canvas & inspector selection state
        clearOldUI();

        selectedBank = bank;
        AppState.getInstance().setSelectedBank(bank);
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
            // fldBankName removed: rely solely on cmbBankAccount
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
            if (lblCustomWidth != null)
                lblCustomWidth.setText("Custom Width (in)");
            if (lblCustomHeight != null)
                lblCustomHeight.setText("Custom Height (in)");
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
            if (lblCustomWidth != null)
                lblCustomWidth.setText("Custom Width (in)");
            if (lblCustomHeight != null)
                lblCustomHeight.setText("Custom Height (in)");
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

        if (bank.getId() != null && bank.getId() > 0) {
            loadTemplateFromBackend(bank.getId().longValue());
        }
    }

    private void clearForm() {
        selectedBank = null;
        if (lblFormTitle != null)
            lblFormTitle.setText("Cheque Template Designer");
        if (btnSave != null)
            btnSave.setText("ðŸ’¾ Save Template");
        if (btnDelete != null)
            btnDelete.setDisable(true);
        if (btnNewBank != null) {
            btnNewBank.setVisible(false);
            btnNewBank.setManaged(false);
        }
        if (btnClear != null) {
            btnClear.setText("â†» Reset Coordinates");
            btnClear.setVisible(true);
            btnClear.setManaged(true);
            btnClear.getStyleClass().remove("btn-primary");
            if (!btnClear.getStyleClass().contains("btn-secondary")) {
                btnClear.getStyleClass().add("btn-secondary");
            }
        }

        isUpdatingForm = true;
        try {
            if (cmbBankAccount != null) {
                cmbBankAccount.setValue(null);
            }
        } finally {
            isUpdatingForm = false;
        }
        if (fldBankCode != null) {
            fldBankCode.clear();
        }
        if (chkMicr != null)
            chkMicr.setSelected(true);
        if (cmbChequeSize != null)
            cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        if (fldCustomWidth != null)
            fldCustomWidth.clear();
        if (fldCustomHeight != null)
            fldCustomHeight.clear();

        currentLayout = new BankTemplateLayout(ChequeSizePreset.STANDARD.getWidthInches(),
                ChequeSizePreset.STANDARD.getHeightInches());
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

    private Bank buildDraftBank() {
        Bank bank = new Bank();
        BankAccount acc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        String name = acc != null && acc.getBankName() != null ? acc.getBankName().trim() : "Bank";
        bank.setBankName(name);
        bank.setBankCode(
                fldBankCode != null && fldBankCode.getText() != null ? fldBankCode.getText().trim().toUpperCase()
                        : "BANK");
        bank.setMicr(chkMicr != null && chkMicr.isSelected());
        return bank;
    }

    // â”€â”€ Canvas layout â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void layoutPreviewPane() {
        if (chequePreviewPane == null || currentLayout == null)
            return;
        double viewW = previewViewport != null && previewViewport.getWidth() > 0
                ? previewViewport.getWidth() : 760.0;
        double viewH = previewViewport != null && previewViewport.getHeight() > 0
                ? previewViewport.getHeight() : 340.0;
        double aspectRatio = currentLayout.getWidthInches() / currentLayout.getHeightInches();
        double paneW = viewW * 0.94;
        double paneH = paneW / aspectRatio;
        if (paneH > viewH * 0.92) {
            paneH = viewH * 0.92;
            paneW = paneH * aspectRatio;
        }
        paneW = Math.max(300.0, paneW);
        paneH = Math.max(120.0, paneH);
        chequePreviewPane.setPrefSize(paneW, paneH);
        chequePreviewPane.setMinSize(paneW, paneH);
        chequePreviewPane.setMaxSize(paneW, paneH);
        // Clear position cache so refreshPreview always recomputes after a resize
        lastPositions.clear();
        if (fieldNodes.isEmpty()) {
            buildFieldNodes(paneW, paneH);
        } else {
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
                node.setLayoutX(clamp(x, 0, Math.max(0, paneW - w)));
                node.setLayoutY(clamp(y, 0, Math.max(0, paneH - h)));
            }
        }
        updateGridOverlay();
        Bank bank = selectedBank != null ? selectedBank : buildDraftBank();
        com.chequeprint.engine.ChequeRenderEngine.initializePreviewElements(previewPane);
        com.chequeprint.engine.ChequeRenderEngine.renderCheque(
                previewPane, AppState.getInstance().getCurrentCheque(), bank, currentLayout);
    }

    private void buildFieldNodes(double paneW, double paneH) {
        chequePreviewPane.getChildren().clear();
        fieldNodes.clear();
        guideLineV = new Line();
        guideLineV.setStroke(javafx.scene.paint.Color.rgb(37, 99, 235, 0.7));
        guideLineV.setStrokeWidth(1.0);
        guideLineV.setVisible(false);
        guideLineV.getStrokeDashArray().addAll(6.0, 4.0);
        guideLineH = new Line();
        guideLineH.setStroke(javafx.scene.paint.Color.rgb(37, 99, 235, 0.7));
        guideLineH.setStrokeWidth(1.0);
        guideLineH.setVisible(false);
        guideLineH.getStrokeDashArray().addAll(6.0, 4.0);
        chequePreviewPane.getChildren().addAll(guideLineV, guideLineH);
        currentLayout.ensureAllFields();
        for (LayoutField field : LayoutField.values()) {
            FieldPosition pos = currentLayout.get(field);
            double x = pos.getXRatio() * paneW;
            double y = pos.getYRatio() * paneH;
            double w = fieldWidthPx(field, pos);
            double h = fieldHeightPx(field, pos);
            Label lbl = new Label(field.name());
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
            lbl.setWrapText(field == LayoutField.AMOUNT_WORDS);
            lbl.setMouseTransparent(true);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setMaxHeight(Double.MAX_VALUE);
            javafx.scene.shape.Circle resizeHandle = new javafx.scene.shape.Circle(6);
            resizeHandle.setFill(javafx.scene.paint.Color.web("#2563eb"));
            resizeHandle.setStroke(javafx.scene.paint.Color.WHITE);
            resizeHandle.setStrokeWidth(1.5);
            resizeHandle.setCursor(Cursor.SE_RESIZE);
            resizeHandle.setVisible(false);
            StackPane.setAlignment(resizeHandle, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(resizeHandle, new Insets(0, 2, 2, 0));
            StackPane node = new StackPane(lbl, resizeHandle);
            node.setPrefSize(w, h);
            node.setMinSize(w, h);
            node.setMaxSize(w, h);
            node.setLayoutX(clamp(x, 0, Math.max(0, paneW - w)));
            node.setLayoutY(clamp(y, 0, Math.max(0, paneH - h)));
            node.setCursor(Cursor.MOVE);
            fieldNodes.put(field, node);
            chequePreviewPane.getChildren().add(node);
            enableDragAndResize(field, node, resizeHandle);
            node.setOnMouseClicked(ev -> {
                setSelectedField(field);
                ev.consume();
            });
        }
        updateFieldHighlights();
    }

    private void enableDragAndResize(LayoutField field, StackPane node, javafx.scene.shape.Circle resizeHandle) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(e -> {
            if (e.getTarget() == resizeHandle) return;
            dragDelta.x = e.getX();
            dragDelta.y = e.getY();
            isDragging = true;
            setSelectedField(field);
            node.toFront();
            e.consume();
        });
        node.setOnMouseDragged(e -> {
            if (e.getTarget() == resizeHandle) return;
            moveFieldNode(field, node, e, dragDelta);
            e.consume();
        });
        node.setOnMouseReleased(e -> {
            isDragging = false;
            if (guideLineV != null) guideLineV.setVisible(false);
            if (guideLineH != null) guideLineH.setVisible(false);
            refreshPreview();
            updateFieldHighlights();
            loadAdjustmentFields(getSelectedField());
            persistCurrentLayoutIfPossible();
            e.consume();
        });
        final Delta resizeDelta = new Delta();
        resizeHandle.setOnMousePressed(e -> {
            resizeDelta.x = e.getScreenX();
            resizeDelta.y = e.getScreenY();
            dragDelta.x = node.getPrefWidth();
            dragDelta.y = node.getPrefHeight();
            isDragging = true;
            setSelectedField(field);
            e.consume();
        });
        resizeHandle.setOnMouseDragged(e -> {
            double dx = e.getScreenX() - resizeDelta.x;
            double dy = e.getScreenY() - resizeDelta.y;
            double newW = Math.max(30.0, dragDelta.x + dx);
            double newH = Math.max(15.0, dragDelta.y + dy);
            if (chkSnapGrid != null && chkSnapGrid.isSelected()) {
                newW = Math.round(newW / 15.0) * 15.0;
                newH = Math.round(newH / 15.0) * 15.0;
            }
            double paneW = chequePreviewPane.getPrefWidth() > 0 ? chequePreviewPane.getPrefWidth() : 720;
            double paneH = chequePreviewPane.getPrefHeight() > 0 ? chequePreviewPane.getPrefHeight() : 300;
            newW = Math.min(newW, paneW - node.getLayoutX());
            newH = Math.min(newH, paneH - node.getLayoutY());
            node.setPrefSize(newW, newH);
            node.setMinSize(newW, newH);
            node.setMaxSize(newW, newH);
            FieldPosition pos = currentLayout.get(field);
            currentLayout.setFieldLayout(field, pos.getXRatio(), pos.getYRatio(), newW / paneW, newH / paneH);
            suppressPreviewListener = true;
            AppState.getInstance().setSelectedTemplate(currentLayout);
            suppressPreviewListener = false;
            updateHUD(field, node);
            loadAdjustmentFields(field);
            e.consume();
        });
        resizeHandle.setOnMouseReleased(e -> {
            isDragging = false;
            refreshPreview();
            updateFieldHighlights();
            loadAdjustmentFields(getSelectedField());
            persistCurrentLayoutIfPossible();
            e.consume();
        });
    }

    private void moveFieldNode(LayoutField field, StackPane node,
            javafx.scene.input.MouseEvent e, Delta dragDelta) {
        double paneW = chequePreviewPane.getPrefWidth() > 0 ? chequePreviewPane.getPrefWidth() : 720;
        double paneH = chequePreviewPane.getPrefHeight() > 0 ? chequePreviewPane.getPrefHeight() : 300;
        double nodeW = node.getPrefWidth();
        double nodeH = node.getPrefHeight();
        double nx = node.getLayoutX() + e.getX() - dragDelta.x;
        double ny = node.getLayoutY() + e.getY() - dragDelta.y;
        if (chkSnapGrid != null && chkSnapGrid.isSelected()) {
            nx = Math.round(nx / 15.0) * 15.0;
            ny = Math.round(ny / 15.0) * 15.0;
        }
        nx = clamp(nx, 0, Math.max(0, paneW - nodeW));
        ny = clamp(ny, 0, Math.max(0, paneH - nodeH));
        node.setLayoutX(nx);
        node.setLayoutY(ny);
        // Snap guide lines
        double cx = nx + nodeW / 2.0, cy = ny + nodeH / 2.0;
        double snapT = 8.0;
        if (guideLineV != null) {
            double pc = paneW / 2.0;
            if (Math.abs(cx - pc) < snapT) {
                nx = pc - nodeW / 2.0;
                node.setLayoutX(nx);
                guideLineV.setStartX(pc); guideLineV.setEndX(pc);
                guideLineV.setStartY(0); guideLineV.setEndY(paneH);
                guideLineV.setVisible(true); guideLineV.toFront();
            } else {
                guideLineV.setVisible(false);
            }
        }
        if (guideLineH != null) {
            double pm = paneH / 2.0;
            if (Math.abs(cy - pm) < snapT) {
                ny = pm - nodeH / 2.0;
                node.setLayoutY(ny);
                guideLineH.setStartX(0); guideLineH.setEndX(paneW);
                guideLineH.setStartY(pm); guideLineH.setEndY(pm);
                guideLineH.setVisible(true); guideLineH.toFront();
            } else {
                guideLineH.setVisible(false);
            }
        }
        currentLayout.setFieldPosition(field, nx / paneW, ny / paneH);
        suppressPreviewListener = true;
        AppState.getInstance().setSelectedTemplate(currentLayout);
        suppressPreviewListener = false;
        Long bankId = Session.getSelectedBankId();
        if (bankId != null && bankTemplateMap != null)
            bankTemplateMap.put(bankId, currentLayout.copy());
        // Debounce: coalesce rapid drag events into one render frame
        schedulePreviewRefresh();
        if (field == getSelectedField())
            loadAdjustmentFields(field);
        updateHUD(field, node);
    }

    private void updateHUD(LayoutField field, StackPane node) {
        if (lblCoordinatesHUD == null || currentLayout == null) return;
        FieldPosition pos = currentLayout.get(field);
        double xMm = pos.getXRatio() * currentLayout.getWidthInches() * 25.4;
        double yMm = pos.getYRatio() * currentLayout.getHeightInches() * 25.4;
        lblCoordinatesHUD.setText(String.format("X: %.1f mm  Y: %.1f mm  W: %.0fpx  H: %.0fpx",
                xMm, yMm, node.getPrefWidth(), node.getPrefHeight()));
    }

    private void refreshPreview() {
        if (currentLayout == null || AppState.getInstance().getSelectedTemplate() == null) {
            for (StackPane n : fieldNodes.values()) n.setVisible(false);
            return;
        }
        currentLayout.ensureAllFields();
        suppressPreviewListener = true;
        AppState.getInstance().setSelectedTemplate(currentLayout);
        suppressPreviewListener = false;
        double paneW = chequePreviewPane.getPrefWidth() > 0 ? chequePreviewPane.getPrefWidth() : 720;
        double paneH = chequePreviewPane.getPrefHeight() > 0 ? chequePreviewPane.getPrefHeight() : 300;
        for (Map.Entry<LayoutField, StackPane> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            StackPane node = entry.getValue();
            FieldPosition pos = currentLayout.get(field);
            double x = pos.getXRatio() * paneW;
            double y = pos.getYRatio() * paneH;
            double w = fieldWidthPx(field, pos);
            double h = fieldHeightPx(field, pos);
            // Dirty-check: skip update when position/size unchanged
            double[] cached = lastPositions.get(field);
            if (cached == null
                    || Math.abs(cached[0] - x) > 0.5 || Math.abs(cached[1] - y) > 0.5
                    || Math.abs(cached[2] - w) > 0.5 || Math.abs(cached[3] - h) > 0.5) {
                node.setPrefSize(w, h);
                node.setMinSize(w, h);
                node.setMaxSize(w, h);
                x = clamp(x, 0, Math.max(0, paneW - w));
                y = clamp(y, 0, Math.max(0, paneH - h));
                node.setLayoutX(x);
                node.setLayoutY(y);
                lastPositions.put(field, new double[]{x, y, w, h});
            }
            node.setVisible(field != LayoutField.MICR || (chkMicr != null && chkMicr.isSelected()));
        }
        if (lblPreviewSize != null) {
            lblPreviewSize.setText(String.format("Preview Size: %.2f x %.2f inches",
                    currentLayout.getWidthInches(), currentLayout.getHeightInches()));
        }
        // During drag/resize skip expensive inspector + highlight â€” applied on release
        if (!isDragging) {
            loadAdjustmentFields(getSelectedField());
            updateFieldHighlights();
            LayoutField sel = getSelectedField();
            if (sel != null) {
                StackPane selNode = fieldNodes.get(sel);
                if (selNode != null) updateHUD(sel, selNode);
            }
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void loadAdjustmentFields(LayoutField field) {

        if (currentLayout == null || field == null || fldAdjustLeft == null) {
            return;
        }

        FieldPosition pos = currentLayout.get(field);
        double widthMm = currentLayout.getWidthInches() * 25.4;
        double heightMm = currentLayout.getHeightInches() * 25.4;

        fldAdjustLeft.setText(formatMm(pos.getXRatio() * widthMm));
        fldAdjustTop.setText(formatMm(pos.getYRatio() * heightMm));
        if (fldAdjustWidth != null)
            fldAdjustWidth.setText(formatMm(effectiveWidthRatio(field, pos) * widthMm));
        if (fldAdjustHeight != null)
            fldAdjustHeight.setText(formatMm(effectiveHeightRatio(field, pos) * heightMm));

        // Load Font and Size properties for selected field
        StackPane node = fieldNodes.get(field);
        if (node != null) {
            for (javafx.scene.Node child : node.getChildren()) {
                if (child instanceof Label label && label.getFont() != null) {
                    String family = label.getFont().getFamily();
                    if (cmbFontFamily != null && family != null && !family.isBlank()) {
                        cmbFontFamily.setValue(family);
                    }
                    if (fldFontSize != null) {
                        int size = (int) Math.round(label.getFont().getSize());
                        fldFontSize.setText(String.valueOf(size));
                    }
                }
            }
        }
    }

    private double fieldWidthPx(LayoutField field, FieldPosition pos) {
        double w = chequePreviewPane.getPrefWidth();
        if (w <= 0)
            w = 720;
        return Math.max(24.0, effectiveWidthRatio(field, pos) * w);
    }

    private double fieldHeightPx(LayoutField field, FieldPosition pos) {
        double h = chequePreviewPane.getPrefHeight();
        if (h <= 0)
            h = 300;
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

    private void persistCurrentLayoutIfPossible() {
        if (currentLayout == null || !currentLayout.isValidLayout()) {
            return;
        }

        String code = selectedBank != null ? safeCode(selectedBank.getBankCode())
                : (fldBankCode != null ? safeCode(fldBankCode.getText()) : "BANK");
        if (code.isBlank()) {
            return;
        }

        final BankTemplateLayout layoutToSave = currentLayout.copy();
        new Thread(() -> {
            try {
                layoutByBankCode.put(code, layoutToSave);
                bankService.saveLayouts(layoutByBankCode);

                Long bankId = Session.getSelectedBankId();
                if (bankId != null && bankId > 0) {
                    List<Map<String, Object>> reloaded = bankService.getTemplateFields(bankId);
                    Platform.runLater(() -> {
                        if (reloaded != null && !reloaded.isEmpty()) {
                            applyReloadedFields(reloaded);
                        }
                        AppState.getInstance()
                                .setSelectedTemplate(currentLayout != null ? currentLayout.copy() : layoutToSave);
                    });
                } else {
                    Platform.runLater(() -> AppState.getInstance().setSelectedTemplate(layoutToSave));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Layout Save Error",
                        "Unable to save cheque alignment: " + ex.getMessage(), Alert.AlertType.ERROR));
            }
        }, "persist-layout").start();
    }

    private void updateGridOverlay() {
        if (chequePreviewPane == null) {
            return;
        }

        boolean showGrid = chkShowGrid != null && chkShowGrid.isSelected();
        boolean showRulers = chkShowRulers != null && chkShowRulers.isSelected();

        StringBuilder style = new StringBuilder();
        style.append(
                "-fx-border-color: #475569; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-border-radius: 6px; ");

        if (showGrid) {
            style.append("-fx-background-color: #ffffff, ");
            style.append(
                    "linear-gradient(from 0px 0px to 15px 0px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px), ");
            style.append(
                    "linear-gradient(from 0px 0px to 0px 15px, repeat, rgba(148,163,184,0.12) 0px, rgba(148,163,184,0.12) 1px, transparent 1px, transparent 15px); ");
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
        if (currentTemplate != null) {
            renderPreview(currentTemplate);
        }
    }

    @FXML
    private void onToggleRulers() {
        updateGridOverlay();
        if (currentTemplate != null) {
            renderPreview(currentTemplate);
        }
    }

    private void updateFieldHighlights() {
        LayoutField selected = getSelectedField();

        setLayerButtonSelected(layerDate, selected == LayoutField.DATE);
        setLayerButtonSelected(layerPayee, selected == LayoutField.PAYEE);
        setLayerButtonSelected(layerAmountNumber, selected == LayoutField.AMOUNT_NUMBER);
        setLayerButtonSelected(layerAmountWords, selected == LayoutField.AMOUNT_WORDS);
        setLayerButtonSelected(layerSignature, selected == LayoutField.SIGNATURE);
        setLayerButtonSelected(layerBankLogo, selected == LayoutField.BANK_LOGO);
        setLayerButtonSelected(layerMicr, selected == LayoutField.MICR);

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
                node.setStyle(baseStyle
                        + " -fx-border-color:#2563eb; -fx-border-style:dashed; -fx-border-width:2px; -fx-background-radius:4; -fx-border-radius:4; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.35), 6, 0, 0, 0);");
                if (resizeHandle != null)
                    resizeHandle.setVisible(true);
            } else {
                node.setStyle(baseStyle + " -fx-border-width:1px; -fx-background-radius:4; -fx-border-radius:4;");
                if (resizeHandle != null)
                    resizeHandle.setVisible(false);
            }
        }
    }

    @FXML
    private void onAlignLeft() {
        alignSelected(0.0, -1);
    }

    @FXML
    private void onAlignRight() {
        alignSelected(1.0, -1);
    }

    @FXML
    private void onCenterHorizontal() {
        alignSelected(0.5, -1);
    }

    @FXML
    private void onAlignTop() {
        alignSelected(-1, 0.0);
    }

    @FXML
    private void onAlignBottom() {
        alignSelected(-1, 1.0);
    }

    @FXML
    private void onCenterVertical() {
        alignSelected(-1, 0.5);
    }

    private void alignSelected(double targetX, double targetY) {
        if (currentLayout == null)
            return;
        LayoutField field = getSelectedField();
        if (field == null)
            return;

        StackPane node = fieldNodes.get(field);
        if (node == null)
            return;

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0)
            paneW = 720;
        if (paneH <= 0)
            paneH = 300;

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

        BankTemplateLayout defaultLayout = new BankTemplateLayout(currentLayout.getWidthInches(),
                currentLayout.getHeightInches());
        FieldPosition defaultPos = defaultLayout.get(field);

        currentLayout.setFieldLayout(field, defaultPos.getXRatio(), defaultPos.getYRatio(), defaultPos.getWidthRatio(),
                defaultPos.getHeightRatio());

        if (cmbFontFamily != null)
            cmbFontFamily.setValue("Arial");
        if (fldFontSize != null)
            fldFontSize.setText("12");

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

            // 1. Update model
            currentLayout.setFieldLayout(field, xRatio, yRatio, widthRatio, heightRatio);

            Long currentBankId = Session.getSelectedBankId();
            if (currentBankId != null && bankTemplateMap != null) {
                bankTemplateMap.put(currentBankId, currentLayout.copy());
            }

            // 2. Update field UI
            String fontFamily = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue()
                    : "Arial";
            int fontSize = getSelectedFontSize();
            applySelectedFieldFont(field, fontFamily, fontSize);

            StackPane node = fieldNodes.get(field);
            if (node != null && chequePreviewPane != null) {
                double paneW = chequePreviewPane.getPrefWidth() > 0 ? chequePreviewPane.getPrefWidth() : 720;
                double paneH = chequePreviewPane.getPrefHeight() > 0 ? chequePreviewPane.getPrefHeight() : 300;
                node.setLayoutX(xRatio * paneW);
                node.setLayoutY(yRatio * paneH);
            }

            refreshPreview();
            persistCurrentLayoutIfPossible();
        } catch (Exception ex) {
            showAlert("Adjustment Error", ex.getMessage(), Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void onPresetSmall() {
        setPresetSize(40.0, 8.0);
    }

    @FXML
    private void onPresetMedium() {
        setPresetSize(80.0, 10.0);
    }

    @FXML
    private void onPresetLarge() {
        setPresetSize(120.0, 12.0);
    }

    @FXML
    private void onPresetFullWidth() {
        if (currentLayout == null)
            return;
        double chequeWmm = currentLayout.getWidthInches() * 25.4;
        setPresetSize(chequeWmm * 0.9, 10.0);
    }

    private void setPresetSize(double widthMm, double heightMm) {
        if (currentLayout == null)
            return;
        LayoutField field = getSelectedField();
        if (field == null)
            return;

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
        fileChooser
                .setInitialFileName((selectedBank != null ? selectedBank.getBankCode() : "layout") + "_template.json");
        File file = fileChooser.showSaveDialog(chequePreviewPane != null && chequePreviewPane.getScene() != null
                ? chequePreviewPane.getScene().getWindow()
                : null);
        if (file != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, currentLayout);
                showAlert("Export Success", "Layout template exported successfully to:\n" + file.getAbsolutePath(),
                        Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export layout JSON: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    public void saveTemplateFieldsToApi(Long templateId) {
        new Thread(() -> saveTemplateFieldsToApiInternal(templateId, true), "save-template-fields-api").start();
    }

    public boolean saveTemplateFieldsToApiInternal(Long templateId, boolean showSuccessAlert) {
        BankAccount selectedAcc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        if (selectedAcc == null || selectedAcc.getId() == null) {
            if (showSuccessAlert) {
                Platform.runLater(() -> showAlert("No Bank Account Selected",
                        "âš ï¸ Please select a valid Bank Account from the dropdown list before saving the cheque template layout.",
                        Alert.AlertType.WARNING));
            }
            return false;
        }

        if (currentLayout != null && !currentLayout.isValidLayout()) {
            if (showSuccessAlert) {
                Platform.runLater(() -> showAlert("Layout Validation Error",
                        "Required fields (Payee, Date, Amount in Figures, Amount in Words) must have valid coordinates before saving.",
                        Alert.AlertType.WARNING));
            }
            return false;
        }

        Long bankAccountId = selectedAcc.getId().longValue();
        templateId = bankAccountId;

        List<Map<String, Object>> directPayload = new ArrayList<>();
        List<Map<String, Object>> fieldsPayload = new ArrayList<>();

        String fontFamily = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue()
                : "Arial";
        int fontSize = 12;
        if (fldFontSize != null && !fldFontSize.getText().isBlank()) {
            try {
                fontSize = Integer.parseInt(fldFontSize.getText().trim());
            } catch (Exception ignored) {
            }
        }

        for (Map.Entry<LayoutField, StackPane> entry : fieldNodes.entrySet()) {
            LayoutField field = entry.getKey();
            StackPane node = entry.getValue();

            Map<String, Object> directItem = new HashMap<>();
            directItem.put("key", field.name());
            directItem.put("x", (int) Math.round(node.getLayoutX()));
            directItem.put("y", (int) Math.round(node.getLayoutY()));
            directPayload.add(directItem);

            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("bankAccountId", templateId);
            fieldMap.put("templateId", templateId);
            fieldMap.put("fieldName", mapFieldName(field));
            fieldMap.put("xPosition", node.getLayoutX());
            fieldMap.put("yPosition", node.getLayoutY());
            fieldMap.put("fontSize", fontSize);
            fieldMap.put("fontFamily", fontFamily);
            fieldsPayload.add(fieldMap);
        }

        final Long targetTemplateId = templateId;
        try {
            boolean directSuccess = bankService.saveTemplateFieldsDirect(directPayload);
            boolean fieldsSuccess = bankService.saveTemplateFields(fieldsPayload);

            if (directSuccess || fieldsSuccess) {
                List<Map<String, Object>> reloadedFields = bankService.getTemplateFields(targetTemplateId);

                Platform.runLater(() -> {
                    applyReloadedFields(reloadedFields);
                    if (currentLayout != null) {
                        bankTemplateMap.put(targetTemplateId, currentLayout.copy());
                        AppState.getInstance().setSelectedTemplate(currentLayout.copy());
                    }
                    refreshPreview();
                    if (showSuccessAlert) {
                        showAlert("Template Saved",
                                "âœ… Cheque template coordinates successfully saved & reloaded from backend!",
                                Alert.AlertType.INFORMATION);
                    }
                });
                return true;
            } else {
                if (showSuccessAlert) {
                    Platform.runLater(() -> showAlert("Save Warning",
                            "âš ï¸ Backend server received request but did not confirm template field save.",
                            Alert.AlertType.WARNING));
                }
            }
        } catch (Exception ex) {
            System.err.println(
                    "[Backend API Error] Failed to save/reload template fields from REST API: " + ex.getMessage());
            ex.printStackTrace();
            if (showSuccessAlert) {
                Platform.runLater(() -> showAlert("Save Error",
                        "âŒ Failed to save template layout to server: " + ex.getMessage(), Alert.AlertType.ERROR));
            }
        }
        return false;
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
        if (fldAdjustLeft != null)
            fldAdjustLeft.clear();
        if (fldAdjustTop != null)
            fldAdjustTop.clear();
        if (fldAdjustWidth != null)
            fldAdjustWidth.clear();
        if (fldAdjustHeight != null)
            fldAdjustHeight.clear();
        if (inspectorGrid != null)
            inspectorGrid.setDisable(true);
        if (alignmentPanel != null)
            alignmentPanel.setDisable(true);

        updateFieldHighlights();
    }

    public void loadNewTemplate(Long bankId, Bank bank) {
        if (bankId == null || bankId <= 0) {
            AppState.getInstance().setSelectedTemplate(null);
            this.currentLayout = null;
            refreshPreview();
            return;
        }

        if (!layoutLoadsInFlight.add(bankId)) {
            return;
        }

        // Clear old template and UI selection immediately when bank changes to avoid
        // mixing layouts
        clearOldUI();
        this.currentLayout = new BankTemplateLayout();

        // Check Map<BankId, Template> cache first
        if (bankTemplateMap.containsKey(bankId)) {
            currentLayout = bankTemplateMap.get(bankId).copy();
            AppState.getInstance().setSelectedTemplate(currentLayout);
            layoutPreviewPane();
            refreshPreview();
            layoutLoadsInFlight.remove(bankId);
            return;
        }

        // Show loading indicator when fetching template from server
        ChequePreviewEngine.renderLoadingState(previewPane, "Loading cheque template...");

        new Thread(() -> {
            try {
                // Call API GET /api/template/{bankId} strictly using unique bankId
                List<Map<String, Object>> templates = bankService.getTemplatesByBankId(bankId);
                Long templateId = bankId;
                if (!templates.isEmpty() && templates.get(0).get("id") instanceof Number) {
                    templateId = ((Number) templates.get(0).get("id")).longValue();
                }

                // Call API GET /api/template/fields/{templateId}
                List<Map<String, Object>> fields = bankService.getTemplateFields(templateId);

                final Long targetBankId = bankId;
                Platform.runLater(() -> {
                    try {
                        if (fields != null && !fields.isEmpty()) {
                            applyReloadedFields(fields);
                        } else {
                            // If no template exists for selected bank, initialize standard default template
                            currentLayout = new BankTemplateLayout();
                            currentLayout.ensureAllFields();
                            layoutPreviewPane();
                        }
                        if (currentLayout != null) {
                            bankTemplateMap.put(targetBankId, currentLayout.copy());
                            AppState.getInstance().setSelectedTemplate(currentLayout);
                        }
                    } finally {
                        layoutLoadsInFlight.remove(targetBankId);
                    }
                });
            } catch (Exception e) {
                System.err.println("Multi-bank template load fallback to default standard layout: " + e.getMessage());
                Platform.runLater(() -> {
                    try {
                        currentLayout = new BankTemplateLayout();
                        currentLayout.ensureAllFields();
                        layoutPreviewPane();
                        AppState.getInstance().setSelectedTemplate(currentLayout);
                    } finally {
                        layoutLoadsInFlight.remove(bankId);
                    }
                });
            }
        }, "load-new-template").start();
    }

    private void applyReloadedFields(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty() || currentLayout == null) {
            return;
        }

        double paneW = chequePreviewPane.getPrefWidth();
        double paneH = chequePreviewPane.getPrefHeight();
        if (paneW <= 0)
            paneW = 720;
        if (paneH <= 0)
            paneH = 300;

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
                        node.setLayoutX(x);
                        node.setLayoutY(y);
                        for (javafx.scene.Node child : node.getChildren()) {
                            if (child instanceof Label label) {
                                label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                                label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize
                                        + "px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
                            }
                        }
                        if (!chequePreviewPane.getChildren().contains(node)) {
                            chequePreviewPane.getChildren().add(node);
                        }
                        currentLayout.setFieldPosition(field, x / paneW, y / paneH);
                    }
                }
            }
        }
        if (currentLayout != null) {
            AppState.getInstance().setSelectedTemplate(currentLayout);
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
        if (field == null)
            return;
        StackPane node = fieldNodes.get(field);
        if (node != null) {
            for (javafx.scene.Node child : node.getChildren()) {
                if (child instanceof Label label) {
                    label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                    label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize
                            + "px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
                }
            }
        }
    }

    private int getSelectedFontSize() {
        if (fldFontSize != null && !fldFontSize.getText().isBlank()) {
            try {
                return Integer.parseInt(fldFontSize.getText().trim());
            } catch (Exception ignored) {
            }
        }
        return 12;
    }

}
