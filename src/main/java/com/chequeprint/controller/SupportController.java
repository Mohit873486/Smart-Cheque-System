package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SupportController {
    @FXML private TextField fldSupportName;
    @FXML private TextField fldSupportEmail;
    @FXML private TextArea  fldMessage;
    @FXML private VBox      rootPane;
    @FXML private Label     lblConfirm;

    @FXML
    public void initialize() {
        FxUtils.animateIn(rootPane, 0);
        lblConfirm.setVisible(false);
    }

    @FXML
    private void onSendMessage() {
        String name  = fldSupportName.getText().trim();
        String email = fldSupportEmail.getText().trim();
        String msg   = fldMessage.getText().trim();

        if (name.isEmpty() || email.isEmpty() || msg.isEmpty()) {
            FxUtils.shake(rootPane);
            new Alert(Alert.AlertType.WARNING, "Please fill in all fields.").show();
            return;
        }

        // In production: send via email API / write to DB
        lblConfirm.setText("✅ Message sent! We'll reply to " + email + " within 24 hours.");
        lblConfirm.setVisible(true);
        FxUtils.animateIn(lblConfirm, 0);
        fldSupportName.clear(); fldSupportEmail.clear(); fldMessage.clear();
    }
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
