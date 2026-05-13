package com.chequeprint.controller;

import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SupportController {

    // ================= INPUT FIELDS =================
    @FXML private TextField tfSupportName;
    @FXML private TextField tfSupportEmail;
    @FXML private ComboBox<String> cbSupportTopic;
    @FXML private TextArea taSupportMessage;

    // ================= UI LABELS =================
    @FXML private Label lblSupportStatus;
    @FXML private Label lblAppVersion;
    @FXML private Label lblBuildDate;

    // ================= ROOT =================
    @FXML private VBox rootPane;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        FxUtils.animateIn(rootPane, 0);

        lblSupportStatus.setVisible(false);

        cbSupportTopic.getItems().addAll(
                "Bug Report",
                "Feature Request",
                "Account Issue",
                "Payment Issue",
                "Other"
        );
        cbSupportTopic.setValue("Bug Report");

        // Static app info (later from config file / service)
        lblAppVersion.setText("v2.0.0");
        lblBuildDate.setText("Build: 2026-05-13");
    }

    // ================= SEND MESSAGE =================
    @FXML
    private void onSendMessage() {

        String name = tfSupportName.getText().trim();
        String email = tfSupportEmail.getText().trim();
        String topic = cbSupportTopic.getValue();
        String msg = taSupportMessage.getText().trim();

        // ================= VALIDATION =================
        if (name.isEmpty() || email.isEmpty() || msg.isEmpty()) {
            FxUtils.shake(rootPane);
            new Alert(Alert.AlertType.WARNING,
                    "Please fill all required fields").show();
            return;
        }

        // ================= SUCCESS (placeholder service layer) =================
        lblSupportStatus.setText(
                "✅ Message sent successfully!\n" +
                "Topic: " + topic + "\n" +
                "We will reply to: " + email + " within 24 hours."
        );

        lblSupportStatus.setVisible(true);
        FxUtils.animateIn(lblSupportStatus, 0);

        // clear form
        tfSupportName.clear();
        tfSupportEmail.clear();
        taSupportMessage.clear();
        cbSupportTopic.setValue("Bug Report");
    }
}