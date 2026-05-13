package com.chequeprint.controller;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * BankController — manages Bank Templates CRUD.
 * Linked to: /view/banks.fxml
 */
public class BankController {

    @FXML
    private TextField fldBankName;
    @FXML
    private TextField fldBankCode;
    @FXML
    private ComboBox<String> cmbChequeSize;
    @FXML
    private CheckBox chkMicr;
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
    private VBox rootPane;
    @FXML
    private Label lblFormTitle;

        // ── State ──
    private MainController mainController;

    private final BankDAO dao = new BankDAO();
    private final ObservableList<Bank> data = FXCollections.observableArrayList();
    private Bank selectedBank = null;

    @FXML
    public void initialize() {
        FxUtils.animateIn(rootPane, 0);
        setupTable();
        setupForm();
        loadData();
    }

    private void setupTable() {
        colBankName
                .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBankName()));
        colBankCode
                .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBankCode()));
        colChequeSize
                .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getChequeSize()));
        colMicr.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isMicr() ? "✅ Yes" : "❌ No"));

        bankTable.setItems(data);
        bankTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null)
                        populateForm(sel);
                });
    }

    private void setupForm() {
        cmbChequeSize.setItems(FXCollections.observableArrayList(
                "8.5x3.66in", "A4", "Letter", "Custom"));
        cmbChequeSize.setValue("8.5x3.66in");
        chkMicr.setSelected(true);
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<Bank> list = dao.findAll();
                Platform.runLater(() -> data.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Load error: " + e.getMessage()).show());
            }
        }, "load-banks").start();
    }

    @FXML
    private void onSave() {
        String name = fldBankName.getText().trim();
        String code = fldBankCode.getText().trim();
        if (name.isEmpty() || code.isEmpty()) {
            FxUtils.shake(fldBankName);
            showAlert("Validation", "Bank name and code are required.", Alert.AlertType.WARNING);
            return;
        }
        try {
            if (selectedBank == null) {
                Bank bt = new Bank(name, code,
                        cmbChequeSize.getValue(), chkMicr.isSelected());
                dao.insert(bt);
                showAlert("Success", "Bank template added.", Alert.AlertType.INFORMATION);
            } else {
                selectedBank.setBankName(name);
                selectedBank.setBankCode(code);
                selectedBank.setChequeSize(cmbChequeSize.getValue());
                selectedBank.setMicr(chkMicr.isSelected());
                dao.update(selectedBank);
                showAlert("Success", "Bank template updated.", Alert.AlertType.INFORMATION);
            }
            clearForm();
            loadData();
        } catch (SQLException e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onDelete() {
        Bank sel = bankTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select", "Please select a bank to delete.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete bank: " + sel.getBankName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    dao.delete(sel.getId());
                    clearForm();
                    loadData();
                } catch (SQLException e) {
                    showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void populateForm(Bank bt) {
        selectedBank = bt;
        lblFormTitle.setText("Edit Bank");
        fldBankName.setText(bt.getBankName());
        fldBankCode.setText(bt.getBankCode());
        cmbChequeSize.setValue(bt.getChequeSize() != null ? bt.getChequeSize() : "8.5x3.66in");
        chkMicr.setSelected(bt.isMicr());
        btnSave.setText("💾  Update Bank");
    }

    private void clearForm() {
        selectedBank = null;
        lblFormTitle.setText("Add New Bank");
        fldBankName.clear();
        fldBankCode.clear();
        cmbChequeSize.setValue("8.5x3.66in");
        chkMicr.setSelected(true);
        btnSave.setText("+ Add Bank");
        bankTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }


    public void setMainController(MainController mc) {
    this.mainController = mc;}
}