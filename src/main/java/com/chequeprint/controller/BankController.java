package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
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

public class BankController {

    private static final Logger LOGGER = Logger.getLogger(BankController.class.getName());
    private final ApiService apiService = new ApiService();
    @FXML private TableView<Bank> bankTable;
    @FXML private TableView<BankAccount> accountTable;
    @FXML private Button btnAddAccount;
    @FXML private Button btnEditAccountAction;
    @FXML private Button btnDeleteAccountAction;
    @FXML private VBox emptyState;
    @FXML private VBox loadingSpinner;
    @FXML private VBox previewEmptyState;
    @FXML private Pane previewPane;
    @FXML private VBox previewLoading;
    @FXML private Button btnEditTemplate;
    @FXML private ComboBox<com.chequeprint.model.ChequeTemplate> cmbAccountTemplates;
    @FXML private Label lblTemplateStatus;
    @FXML private Button btnSetAsDefault;
    @FXML private Button btnPreviewTemplate;

    private static final double PREVIEW_PPI = 90.0;
    private final Map<Long, BankTemplateLayout> bankTemplateMap = new java.util.concurrent.ConcurrentHashMap<>();

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

    @FXML
    private void onAddAccount() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/bank_account_dialog.fxml"));
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
            javafx.scene.Parent ownerRoot = accountTable != null && accountTable.getScene() != null ? accountTable.getScene().getRoot() : null;
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
                        if (acc.getAccountNumber() != null && acc.getAccountNumber().equalsIgnoreCase(account.getAccountNumber().trim())) {
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
                        } else {
                            return apiService.saveBankAccount(account);
                        }
                    }
                };
                saveTask.setOnSucceeded(ev -> {
                    BankAccount saved = saveTask.getValue();
                    BankAccount finalObj = saved != null ? saved : account;

                    if (exists && match != null) {
                        int idx = accountData.indexOf(match);
                        if (idx >= 0) {
                            accountData.set(idx, finalObj);
                        } else {
                            accountData.add(finalObj);
                        }
                    } else {
                        accountData.add(finalObj);
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
                    alert.setContentText(exists ? "Account already exists! Details updated successfully." : "Bank account saved successfully to database!");
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/bank_account_dialog.fxml"));
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
                        } else {
                            return apiService.saveBankAccount(updatedAccount);
                        }
                    }
                };
                updateTask.setOnSucceeded(ev -> {
                    BankAccount result = updateTask.getValue();
                    int idx = accountData.indexOf(selected);
                    if (idx >= 0) {
                        accountData.set(idx, result != null ? result : updatedAccount);
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

    private VBox createStyledFormField(String labelText, String placeholder) {
        VBox box = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 9 12; -fx-font-size: 13px;");
        box.getChildren().addAll(lbl, tf);
        return box;
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
            if (latestTemplate != null) {
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
        clearPreviewPane();
        this.currentTemplate = template != null ? template : new com.chequeprint.model.ChequeTemplate();
        renderPreview(this.currentTemplate);
    }

    public void updateFieldPosition(String fieldName, double x, double y, double fontSize, boolean visible) {
        if (currentTemplate == null) {
            currentTemplate = new com.chequeprint.model.ChequeTemplate();
        }

        com.chequeprint.model.ChequeTemplate.FieldConfig cfg = switch (fieldName != null ? fieldName.toLowerCase().trim() : "") {
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
            if (fontSize > 0) cfg.setFontSize(fontSize);
            cfg.setVisible(visible);

            // Re-render live preview instantly in real-time
            setCurrentTemplate(currentTemplate);
        }
    }

    private void updateTemplateMappingLabel(BankAccount account) {
        if (account == null) {
            if (lblTemplateMapping != null) lblTemplateMapping.setText("Select an account to view template mapping");
            if (lblTemplateMappingDesigner != null) lblTemplateMappingDesigner.setText("Select a Bank Account");
            return;
        }

        String bankName = account.getBankName() != null && !account.getBankName().isBlank() ? account.getBankName() : "Bank Account";
        String accNo = account.getAccountNumber() != null ? account.getAccountNumber().trim() : "";
        String last4 = accNo.length() >= 4 ? accNo.substring(accNo.length() - 4) : accNo;
        String labelText = bankName + (last4.isEmpty() ? "" : " (••• " + last4 + ")");

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
                if (isUpdatingForm) return;
                try {
                    isUpdatingForm = true;
                    boolean hasSelection = (newVal != null);
                    if (btnEditAccountAction != null) btnEditAccountAction.setDisable(!hasSelection);
                    if (btnDeleteAccountAction != null) btnDeleteAccountAction.setDisable(!hasSelection);
                    updateTemplateMappingLabel(newVal);

                    if (newVal == null) {
                        if (previewEmptyState != null) previewEmptyState.setVisible(true);
                        if (previewPane != null) {
                            previewPane.setVisible(false);
                            previewPane.getChildren().clear();
                        }
                        if (btnEditTemplate != null) btnEditTemplate.setDisable(true);
                    } else {
                        if (previewEmptyState != null) previewEmptyState.setVisible(false);
                        if (previewPane != null) previewPane.setVisible(true);
                        if (btnEditTemplate != null) btnEditTemplate.setDisable(false);

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
        if (cmbAccountTemplates == null) return;
        if (account == null || account.getId() == null) {
            cmbAccountTemplates.getItems().clear();
            if (btnSetAsDefault != null) btnSetAsDefault.setDisable(true);
            if (btnPreviewTemplate != null) btnPreviewTemplate.setDisable(true);
            if (lblTemplateStatus != null) lblTemplateStatus.setText("NO SELECTION");
            return;
        }

        new Thread(() -> {
            try {
                Long accountId = account.getId().longValue();
                List<com.chequeprint.model.ChequeTemplate> templates = new ArrayList<>();
                try {
                    java.net.http.HttpRequest req = com.chequeprint.util.RestApiClient.requestBuilder(com.chequeprint.config.ApiConfig.BASE_URL + "/api/template/account/" + accountId).GET().build();
                    java.net.http.HttpResponse<String> res = com.chequeprint.util.RestApiClient.send(req);
                    String json = res.statusCode() == 200 ? res.body() : null;
                    if (json != null && !json.isBlank()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        templates = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, com.chequeprint.model.ChequeTemplate.class));
                    }
                } catch (Exception ignored) {}

                if (templates.isEmpty()) {
                    com.chequeprint.model.ChequeTemplate defaultTpl = new com.chequeprint.model.ChequeTemplate();
                    defaultTpl.setId(account.getTemplateId() != null ? account.getTemplateId() : 1L);
                    defaultTpl.setTemplateName(account.getBankName() + " Standard CTS-2010");
                    templates.add(defaultTpl);
                }

                final List<com.chequeprint.model.ChequeTemplate> finalTemplates = templates;
                Platform.runLater(() -> {
                    cmbAccountTemplates.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(com.chequeprint.model.ChequeTemplate t) {
                            if (t == null) return "";
                            boolean isDef = account.getTemplateId() != null && account.getTemplateId().equals(t.getId());
                            return (isDef ? "⭐ [DEFAULT] " : "📄 ") + (t.getTemplateName() != null ? t.getTemplateName() : "Template #" + t.getId());
                        }

                        @Override
                        public com.chequeprint.model.ChequeTemplate fromString(String string) {
                            return null;
                        }
                    });

                    cmbAccountTemplates.setItems(FXCollections.observableArrayList(finalTemplates));

                    com.chequeprint.model.ChequeTemplate defaultItem = finalTemplates.get(0);
                    for (com.chequeprint.model.ChequeTemplate t : finalTemplates) {
                        if (account.getTemplateId() != null && account.getTemplateId().equals(t.getId())) {
                            defaultItem = t;
                            break;
                        }
                    }
                    cmbAccountTemplates.setValue(defaultItem);

                    if (btnSetAsDefault != null) btnSetAsDefault.setDisable(false);
                    if (btnPreviewTemplate != null) btnPreviewTemplate.setDisable(false);

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

    private void updateTemplateStatusBadge(BankAccount account, com.chequeprint.model.ChequeTemplate selectedTpl) {
        if (lblTemplateStatus == null) return;
        boolean isDefault = (account != null && account.getTemplateId() != null && selectedTpl != null && account.getTemplateId().equals(selectedTpl.getId()));
        if (isDefault) {
            lblTemplateStatus.setText("⭐ ACTIVE DEFAULT");
            lblTemplateStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
        } else {
            lblTemplateStatus.setText("OPTIONAL TEMPLATE");
            lblTemplateStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
        }
    }

    @FXML
    private void onSetAsDefaultTemplate() {
        BankAccount selectedAcc = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;
        com.chequeprint.model.ChequeTemplate selectedTpl = cmbAccountTemplates != null ? cmbAccountTemplates.getValue() : null;

        if (selectedAcc == null || selectedTpl == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a Bank Account and Template first.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        try {
            Long accId = selectedAcc.getId().longValue();
            Long tplId = selectedTpl.getId();

            try {
                java.net.http.HttpRequest req = com.chequeprint.util.RestApiClient.requestBuilder(com.chequeprint.config.ApiConfig.BASE_URL + "/api/template/account/" + accId + "/default/" + tplId)
                        .PUT(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
                com.chequeprint.util.RestApiClient.send(req);
            } catch (Exception ignored) {}

            selectedAcc.setTemplateId(tplId);
            AppState.getInstance().setSelectedBankAccount(selectedAcc);

            updateTemplateStatusBadge(selectedAcc, selectedTpl);
            cmbAccountTemplates.setItems(FXCollections.observableArrayList(cmbAccountTemplates.getItems()));
            cmbAccountTemplates.setValue(selectedTpl);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Template '" + selectedTpl.getTemplateName() + "' is now the active DEFAULT template for " + selectedAcc.getBankName() + "!", ButtonType.OK);
            alert.setTitle("Default Template Updated");
            alert.setHeaderText(null);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update default template: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void onPreviewSelectedTemplate() {
        com.chequeprint.model.ChequeTemplate selectedTpl = cmbAccountTemplates != null ? cmbAccountTemplates.getValue() : null;
        BankAccount selectedAcc = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;

        if (selectedTpl == null && selectedAcc != null && selectedAcc.getId() != null) {
            loadTemplateFromBackend(selectedAcc.getId().longValue());
            return;
        }
        if (selectedTpl != null) {
            setCurrentTemplate(selectedTpl);
            if (previewEmptyState != null) previewEmptyState.setVisible(false);
            if (previewPane != null) previewPane.setVisible(true);
        }
    }

    private final Map<String, com.chequeprint.model.ChequeTemplate> templateCache = new java.util.concurrent.ConcurrentHashMap<>();

    @FXML
    private void onSaveTemplate() {
        if (currentTemplate == null) {
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

        // Show loading spinner during save
        if (previewLoading != null) {
            previewLoading.setVisible(true);
        }

        Task<com.chequeprint.model.ChequeTemplate> saveTask = new Task<>() {
            @Override
            protected com.chequeprint.model.ChequeTemplate call() throws Exception {
                currentTemplate.updateConfigJson();
                return apiService.saveChequeTemplate(currentTemplate);
            }
        };

        saveTask.setOnSucceeded(e -> {
            // Hide loading spinner
            if (previewLoading != null) {
                previewLoading.setVisible(false);
            }
            com.chequeprint.model.ChequeTemplate saved = saveTask.getValue();
            if (saved != null) {
                // Reload fresh template from REST API and refresh preview
                if (saved.getBankId() != null) {
                    templateCache.remove(String.valueOf(saved.getBankId()));
                    loadTemplateFromBackend(saved.getBankId());
                } else {
                    setCurrentTemplate(saved);
                }
            }

            // Success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Template Saved");
            alert.setHeaderText("Success!");
            alert.setContentText("Cheque template updated successfully in database.");
            alert.showAndWait();
        });

        saveTask.setOnFailed(e -> {
            // Hide loading spinner
            if (previewLoading != null) {
                previewLoading.setVisible(false);
            }
            Throwable ex = saveTask.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to Save Cheque Template");
            alert.setContentText(ex != null ? ex.getMessage() : "Unable to connect to Spring Boot REST server.");
            alert.showAndWait();
        });

        new Thread(saveTask).start();
    }



    @FXML
    private void onPrintCheque() {
        if (currentTemplate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Print Precondition Failed");
            alert.setHeaderText("No Template Loaded");
            alert.setContentText("No cheque template loaded for printing. Please select a bank layout first.");
            alert.showAndWait();
            return;
        }

        LOGGER.info("[BankController] Initiating background cheque printing job...");

        if (previewLoading != null) {
            previewLoading.setVisible(true);
        }

        Pane printCanvas = new Pane();
        printCanvas.setPrefWidth(currentTemplate.getWidth() * 3.7795); // Convert mm to points
        printCanvas.setPrefHeight(currentTemplate.getHeight() * 3.7795);

        javafx.scene.Node printableNode = previewPane != null ? previewPane : printCanvas;
        javafx.stage.Window ownerWindow = previewPane != null && previewPane.getScene() != null ?
                previewPane.getScene().getWindow() : null;

        Task<Boolean> printTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                LOGGER.info("[BankController] Starting background printing task on thread: " + Thread.currentThread().getName());
                boolean printed = printService.printNode(printableNode, ownerWindow);
                LOGGER.info("[BankController] Background printing task completed with result: " + printed);
                return printed;
            }
        };

        printTask.setOnSucceeded(e -> {
            if (previewLoading != null) {
                previewLoading.setVisible(false);
            }
            boolean printed = printTask.getValue();
            LOGGER.info("[BankController] Print task succeeded callback executed on FX thread. Printed: " + printed);

            if (printed) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Print Completed");
                alert.setHeaderText("Success!");
                alert.setContentText("Cheque template sent to printer with precise field alignment.");
                alert.showAndWait();
            }
        });

        printTask.setOnFailed(e -> {
            if (previewLoading != null) {
                previewLoading.setVisible(false);
            }
            Throwable ex = printTask.getException();
            LOGGER.log(java.util.logging.Level.SEVERE, "[BankController] Print task failed on background thread", ex);

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Printing Stopped");
            alert.setHeaderText("Precondition Check Failed");
            alert.setContentText(ex != null ? ex.getMessage() : "An error occurred during printing.");
            alert.showAndWait();
        });

        new Thread(printTask).start();
    }

    private void loadTemplateFromBackend(Long bankId) {
        if (bankId == null) {
            setCurrentTemplate(new com.chequeprint.model.ChequeTemplate());
            return;
        }

        String cacheKey = String.valueOf(bankId);
        if (templateCache.containsKey(cacheKey)) {
            // Serve directly from in-memory cache, skipping redundant REST API calls
            setCurrentTemplate(templateCache.get(cacheKey));
            return;
        }

        if (!templateLoadsInFlight.add(cacheKey)) {
            return;
        }

        if (previewLoading != null) {
            previewLoading.setVisible(true);
            previewLoading.setManaged(true);
        }

        Task<com.chequeprint.model.ChequeTemplate> task = new Task<>() {
            @Override
            protected com.chequeprint.model.ChequeTemplate call() throws Exception {
                return apiService.getChequeTemplateByBankId(bankId);
            }
        };

        task.setOnSucceeded(e -> {
            templateLoadsInFlight.remove(cacheKey);
            if (previewLoading != null) {
                previewLoading.setVisible(false);
                previewLoading.setManaged(false);
            }
            com.chequeprint.model.ChequeTemplate template = task.getValue();
            if (template == null) {
                template = new com.chequeprint.model.ChequeTemplate(bankId, "Default Bank Template");
            }
            if (template.getId() != null) {
                BankAccount sel = accountTable != null ? accountTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    sel.setTemplateId(template.getId());
                }
            }
            templateCache.put(cacheKey, template);
            setCurrentTemplate(template);
        });

        task.setOnFailed(e -> {
            templateLoadsInFlight.remove(cacheKey);
            if (previewLoading != null) {
                previewLoading.setVisible(false);
                previewLoading.setManaged(false);
            }
            System.err.println("Failed to fetch template for bankId " + bankId + ": " + task.getException().getMessage());
            com.chequeprint.model.ChequeTemplate defaultTemplate = new com.chequeprint.model.ChequeTemplate(bankId, "Default Bank Template");
            templateCache.put(cacheKey, defaultTemplate);
            setCurrentTemplate(defaultTemplate);
        });

        new Thread(task).start();
    }

    private void renderGridOverlay(Pane pane, double spacing) {
        if (pane == null) return;
        double w = pane.getWidth() > 0 ? pane.getWidth() : (pane.getPrefWidth() > 0 ? pane.getPrefWidth() : 300.0);
        double h = pane.getHeight() > 0 ? pane.getHeight() : (pane.getPrefHeight() > 0 ? pane.getPrefHeight() : 170.0);

        for (double x = spacing; x < w; x += spacing) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, 0, x, h);
            line.setStroke(javafx.scene.paint.Color.web("#cbd5e1", 0.45));
            line.getStrokeDashArray().addAll(2.0, 2.0);
            line.setMouseTransparent(true);
            pane.getChildren().add(line);
        }

        for (double y = spacing; y < h; y += spacing) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, w, y);
            line.setStroke(javafx.scene.paint.Color.web("#cbd5e1", 0.45));
            line.getStrokeDashArray().addAll(2.0, 2.0);
            line.setMouseTransparent(true);
            pane.getChildren().add(line);
        }
    }

    public void renderPreview(com.chequeprint.model.ChequeTemplate template) {
        if (previewPane == null) return;
        com.chequeprint.util.ChequeRenderEngine.renderCheque(previewPane, AppState.getInstance().getCurrentCheque(), selectedBank, AppState.getInstance().getSelectedTemplate());
    }

    private String selectedFieldName = null;

    public String getSelectedFieldName() {
        return selectedFieldName;
    }

    public void selectField(String fieldName) {
        this.selectedFieldName = fieldName;
        if (currentTemplate != null) {
            renderPreview(currentTemplate);
        }
    }

    private void renderAlignmentGuides(Pane pane, double centerX, double centerY) {
        if (pane == null) return;
        removeAlignmentGuides(pane);

        double w = pane.getWidth() > 0 ? pane.getWidth() : (pane.getPrefWidth() > 0 ? pane.getPrefWidth() : 300.0);
        double h = pane.getHeight() > 0 ? pane.getHeight() : (pane.getPrefHeight() > 0 ? pane.getPrefHeight() : 170.0);

        javafx.scene.shape.Line horizGuide = new javafx.scene.shape.Line(0, centerY, w, centerY);
        horizGuide.setStroke(javafx.scene.paint.Color.web("#2563eb", 0.75));
        horizGuide.getStrokeDashArray().addAll(4.0, 4.0);
        horizGuide.setStrokeWidth(1.2);
        horizGuide.setId("alignment-guide-horiz");
        horizGuide.setMouseTransparent(true);

        javafx.scene.shape.Line vertGuide = new javafx.scene.shape.Line(centerX, 0, centerX, h);
        vertGuide.setStroke(javafx.scene.paint.Color.web("#2563eb", 0.75));
        vertGuide.getStrokeDashArray().addAll(4.0, 4.0);
        vertGuide.setStrokeWidth(1.2);
        vertGuide.setId("alignment-guide-vert");
        vertGuide.setMouseTransparent(true);

        pane.getChildren().addAll(horizGuide, vertGuide);
    }

    private void removeAlignmentGuides(Pane pane) {
        if (pane == null) return;
        pane.getChildren().removeIf(node -> "alignment-guide-horiz".equals(node.getId()) || "alignment-guide-vert".equals(node.getId()));
    }

    private void makeLabelDraggable(Label label, String fieldKey, com.chequeprint.model.ChequeTemplate.FieldConfig cfg, double scaleX, double scaleY) {
        if (label == null || cfg == null) return;
        label.setCursor(javafx.scene.Cursor.HAND);
        final double[] dragOffset = new double[2];

        label.setOnMousePressed(event -> {
            dragOffset[0] = event.getX();
            dragOffset[1] = event.getY();
            this.selectedFieldName = fieldKey;
            label.toFront();
            if (currentTemplate != null) {
                renderPreview(currentTemplate);
            }
            event.consume();
        });

        label.setOnMouseDragged(event -> {
            double newLayoutX = label.getLayoutX() + event.getX() - dragOffset[0];
            double newLayoutY = label.getLayoutY() + event.getY() - dragOffset[1];

            if (previewPane != null) {
                newLayoutX = Math.max(0, Math.min(newLayoutX, previewPane.getWidth() - label.getWidth()));
                newLayoutY = Math.max(0, Math.min(newLayoutY, previewPane.getHeight() - label.getHeight()));
            }

            label.setLayoutX(newLayoutX);
            label.setLayoutY(newLayoutY);

            double mmX = newLayoutX / scaleX;
            double mmY = newLayoutY / scaleY;

            cfg.setX(mmX);
            cfg.setY(mmY);

            if (currentLayout != null && previewPane != null) {
                double pw = previewPane.getWidth() > 0 ? previewPane.getWidth() : 720.0;
                double ph = previewPane.getHeight() > 0 ? previewPane.getHeight() : 300.0;
                LayoutField f = unmapFieldName(fieldKey);
                if (f != null) {
                    currentLayout.setFieldPosition(f, newLayoutX / pw, newLayoutY / ph);
                    AppState.getInstance().setSelectedTemplate(currentLayout);
                }
            }

            if (lblCoordinatesHUD != null) {
                lblCoordinatesHUD.setText(String.format("X: %.1f mm | Y: %.1f mm", mmX, mmY));
            }

            renderAlignmentGuides(previewPane, newLayoutX + label.getWidth() / 2.0, newLayoutY + label.getHeight() / 2.0);
            event.consume();
        });

        label.setOnMouseReleased(event -> {
            removeAlignmentGuides(previewPane);
            if (currentTemplate != null) {
                renderPreview(currentTemplate);
            }
            event.consume();
        });
    }

    private double scalePos(double val, double scale, double fallback) {
        if (val < 0) return fallback;
        return val * scale;
    }

    private String formatFieldStyle(String fieldKey, com.chequeprint.model.ChequeTemplate.FieldConfig cfg) {
        if (cfg == null) return "-fx-font-size: 10px; -fx-text-fill: #0f172a;";
        double sz = cfg.getFontSize() > 0 ? Math.min(cfg.getFontSize(), 14.0) : 10.0;
        String family = cfg.getFontFamily() != null ? cfg.getFontFamily() : "Arial";
        String weight = cfg.isBold() ? "bold" : "normal";
        String posture = cfg.isItalic() ? "italic" : "normal";

        String highlight = "";
        if (fieldKey != null && fieldKey.equalsIgnoreCase(selectedFieldName)) {
            highlight = "; -fx-border-color: #2563eb; -fx-border-width: 1.5px; -fx-border-style: dashed; -fx-background-color: rgba(37,99,235,0.15); -fx-padding: 2 4; -fx-border-radius: 4; -fx-background-radius: 4;";
        }
        return String.format("-fx-font-size: %.1fpx; -fx-font-family: '%s'; -fx-font-weight: %s; -fx-font-style: %s; -fx-text-fill: #0f172a;", sz, family, weight, posture) + highlight;
    }

    private void setupForm() {
        if (cmbBankAccount != null) {
            cmbBankAccount.setItems(accountData);
            cmbBankAccount.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdatingForm) return;
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

        if (cmbChequeSize != null) {
            cmbChequeSize.setItems(FXCollections.observableArrayList(ChequeSizePreset.values()));
            cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        }
        if (chkMicr != null) chkMicr.setSelected(true);

        if (fldCustomWidth != null) fldCustomWidth.setDisable(true);
        if (fldCustomHeight != null) fldCustomHeight.setDisable(true);

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
            node.toFront();
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
        AppState.getInstance().setSelectedTemplate(currentLayout);

        Long currentBankId = Session.getSelectedBankId();
        if (currentBankId != null && bankTemplateMap != null && currentLayout != null) {
            bankTemplateMap.put(currentBankId, currentLayout.copy());
        }

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
        if (currentLayout == null || AppState.getInstance().getSelectedTemplate() == null) {
            for (StackPane node : fieldNodes.values()) {
                node.setVisible(false);
            }
            return;
        }
        currentLayout.ensureAllFields();
        AppState.getInstance().setSelectedTemplate(currentLayout);

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
            if (cmbBankAccount != null && cmbBankAccount.getScene() != null && cmbBankAccount.getScene().getRoot() != null) {
                cmbBankAccount.getScene().getRoot().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
            } else if (fldBankCode != null && fldBankCode.getScene() != null && fldBankCode.getScene().getRoot() != null) {
                fldBankCode.getScene().getRoot().setCursor(loading ? Cursor.WAIT : Cursor.DEFAULT);
            }
            if (btnSave != null) btnSave.setDisable(loading);
            if (btnDelete != null) btnDelete.setDisable(loading || selectedBank == null);
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
                    if (cmbBankAccount.getValue() == null || !targetAcc.getId().equals(cmbBankAccount.getValue().getId())) {
                        cmbBankAccount.setValue(targetAcc);
                    }
                    AppState.getInstance().setSelectedBankAccount(targetAcc);
                    if (targetAcc.getId() != null) {
                        loadNewTemplate(targetAcc.getId().longValue(), new Bank(targetAcc.getBankName(), targetAcc.getBankName(), "STANDARD", true));
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
        BankAccount selectedAcc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        if (selectedAcc == null || selectedAcc.getId() == null) {
            showAlert("No Bank Account Selected", "⚠️ Please select a valid Bank Account from the dropdown list before saving the cheque template.", Alert.AlertType.WARNING);
            return;
        }

        String name = selectedAcc.getBankName() != null ? selectedAcc.getBankName().trim() : "Bank";
        String code = selectedAcc.getAccountNumber() != null && !selectedAcc.getAccountNumber().isBlank() ? selectedAcc.getAccountNumber().trim().toUpperCase() : "BANK";
        Integer bankId = selectedAcc.getId();

        ChequeSizePreset preset = cmbChequeSize.getValue();
        BankTemplateLayout formLayout = buildLayoutFromFormSize();
        if (formLayout == null) {
            return;
        }

        String encodedSize = ChequeSizeCodec.encode(preset, formLayout.getWidthInches(), formLayout.getHeightInches());

        Bank bank = selectedBank;
        if (bank == null) {
            bank = new Bank(name, code, encodedSize, chkMicr.isSelected());
            bank.setId(bankId);
        } else {
            bank.setId(bankId);
            bank.setBankName(name);
            bank.setBankCode(code);
            bank.setChequeSize(encodedSize);
            bank.setMicr(chkMicr.isSelected());
        }

        final Bank finalBank = bank;
        final Long targetBankId = bankId != null ? bankId.longValue() : 1L;
        final BankTemplateLayout layoutToSave = currentLayout != null ? currentLayout.copy() : formLayout.copy();

        String formattedIfsc = formatValidIfsc(code);

        setLoading(true);
        new Thread(() -> {
            try {
                // 2. Call Bank Template Local/REST Service
                bankService.save(finalBank, layoutToSave, layoutByBankCode);

                // 3. Save Template Fields to REST API automatically assigning target bank account ID
                saveTemplateFieldsToApi(targetBankId);

                // 4. Show Success Alert & Refresh Table
                Platform.runLater(() -> {
                    loadData();
                    loadBankAccounts();
                    showAlert("Success", "Bank template & field positions saved successfully for account: " + name, Alert.AlertType.INFORMATION);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Save Error", "Failed to save bank template: " + e.getMessage(), Alert.AlertType.ERROR));
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

        if (bank.getId() != null && bank.getId() > 0) {
            loadTemplateFromBackend(bank.getId().longValue());
        }
    }

    private void clearForm() {
        selectedBank = null;
        if (lblFormTitle != null) lblFormTitle.setText("Cheque Template Designer");
        if (btnSave != null) btnSave.setText("💾 Save Template");
        if (btnDelete != null) btnDelete.setDisable(true);
        if (btnNewBank != null) {
            btnNewBank.setVisible(false);
            btnNewBank.setManaged(false);
        }
        if (btnClear != null) {
            btnClear.setText("↻ Reset Coordinates");
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
        if (chkMicr != null) chkMicr.setSelected(true);
        if (cmbChequeSize != null) cmbChequeSize.setValue(ChequeSizePreset.STANDARD);
        if (fldCustomWidth != null) fldCustomWidth.clear();
        if (fldCustomHeight != null) fldCustomHeight.clear();

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
        BankAccount acc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        String name = acc != null && acc.getBankName() != null ? acc.getBankName().trim() : "Bank";
        bank.setBankName(name);
        bank.setBankCode(fldBankCode != null && fldBankCode.getText() != null ? fldBankCode.getText().trim().toUpperCase() : "BANK");
        bank.setMicr(chkMicr != null && chkMicr.isSelected());
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
        if (fldAdjustWidth != null) fldAdjustWidth.setText(formatMm(effectiveWidthRatio(field, pos) * widthMm));
        if (fldAdjustHeight != null) fldAdjustHeight.setText(formatMm(effectiveHeightRatio(field, pos) * heightMm));

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
        if (currentLayout == null || !currentLayout.isValidLayout()) {
            return;
        }

        String code = selectedBank != null ? safeCode(selectedBank.getBankCode()) : (fldBankCode != null ? safeCode(fldBankCode.getText()) : "BANK");
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
                        AppState.getInstance().setSelectedTemplate(currentLayout != null ? currentLayout.copy() : layoutToSave);
                    });
                } else {
                    Platform.runLater(() -> AppState.getInstance().setSelectedTemplate(layoutToSave));
                }
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

            // 1. Update model
            currentLayout.setFieldLayout(field, xRatio, yRatio, widthRatio, heightRatio);

            Long currentBankId = Session.getSelectedBankId();
            if (currentBankId != null && bankTemplateMap != null) {
                bankTemplateMap.put(currentBankId, currentLayout.copy());
            }

            // 2. Update field UI
            String fontFamily = cmbFontFamily != null && cmbFontFamily.getValue() != null ? cmbFontFamily.getValue() : "Arial";
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
        BankAccount selectedAcc = cmbBankAccount != null ? cmbBankAccount.getValue() : null;
        if (selectedAcc == null || selectedAcc.getId() == null) {
            showAlert("No Bank Account Selected", "⚠️ Please select a valid Bank Account from the dropdown list before saving the cheque template layout.", Alert.AlertType.WARNING);
            return;
        }

        if (currentLayout != null && !currentLayout.isValidLayout()) {
            showAlert("Layout Validation Error", "Required fields (Payee, Date, Amount in Figures, Amount in Words) must have valid coordinates before saving.", Alert.AlertType.WARNING);
            return;
        }

        Long bankAccountId = selectedAcc.getId().longValue();
        templateId = bankAccountId;

        List<Map<String, Object>> directPayload = new ArrayList<>();
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

            // 1. Create JSON item: { "key": "DATE", "x": 100, "y": 50 }
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
        System.out.println("[Backend API Request] POST /api/template-fields for bankAccountId=" + targetTemplateId);
        System.out.println("[Backend API Payload] " + fieldsPayload);

        new Thread(() -> {
            try {
                // 3. Send API: POST /api/template-fields
                boolean directSuccess = bankService.saveTemplateFieldsDirect(directPayload);
                boolean fieldsSuccess = bankService.saveTemplateFields(fieldsPayload);

                System.out.println("[Backend API Response] directSuccess=" + directSuccess + ", fieldsSuccess=" + fieldsSuccess);

                if (directSuccess || fieldsSuccess) {
                    List<Map<String, Object>> reloadedFields = bankService.getTemplateFields(targetTemplateId);
                    System.out.println("[Backend API Response] Reloaded " + (reloadedFields != null ? reloadedFields.size() : 0) + " template fields.");

                    Platform.runLater(() -> {
                        applyReloadedFields(reloadedFields);
                        if (currentLayout != null) {
                            bankTemplateMap.put(targetTemplateId, currentLayout.copy());
                            AppState.getInstance().setSelectedTemplate(currentLayout.copy());
                        }
                        refreshPreview();
                        showAlert("Template Saved", "✅ Cheque template coordinates successfully saved & reloaded from backend!", Alert.AlertType.INFORMATION);
                    });
                } else {
                    System.err.println("[Backend API Warning] Server returned false for template fields save.");
                    Platform.runLater(() -> showAlert("Save Warning", "⚠️ Backend server received request but did not confirm template field save.", Alert.AlertType.WARNING));
                }
            } catch (Exception ex) {
                System.err.println("[Backend API Error] Failed to save/reload template fields from REST API: " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> showAlert("Save Error", "❌ Failed to save template layout to server: " + ex.getMessage(), Alert.AlertType.ERROR));
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
            AppState.getInstance().setSelectedTemplate(null);
            this.currentLayout = null;
            refreshPreview();
            return;
        }

        if (!layoutLoadsInFlight.add(bankId)) {
            return;
        }

        // Clear old template and UI selection immediately when bank changes to avoid mixing layouts
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
                        // Step 3: Set layoutX and layoutY
                        node.setLayoutX(x);
                        node.setLayoutY(y);

                        // Configure Label
                        for (javafx.scene.Node child : node.getChildren()) {
                            if (child instanceof Label label) {
                                label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                                label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
                            }
                        }

                        // Step 4: Add to canvas
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
        if (field == null) return;
        StackPane node = fieldNodes.get(field);
        if (node != null) {
            for (javafx.scene.Node child : node.getChildren()) {
                if (child instanceof Label label) {
                    label.setFont(javafx.scene.text.Font.font(fontFamily, fontSize));
                    label.setStyle("-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + "px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
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

