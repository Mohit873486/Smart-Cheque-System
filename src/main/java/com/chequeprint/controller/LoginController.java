package com.chequeprint.controller;

import com.chequeprint.service.UserService;
import com.chequeprint.util.DBConnection;
import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * LoginController — credential screen shown before main app.
 *
 * LAYOUT FIX:
 *   After successful login the main window is sized to 1280×800 AND then
 *   stage.setMaximized(true) is called. This is the correct way to launch a
 *   JavaFX admin dashboard in full-screen mode. Without setMaximized(true) the
 *   BorderPane root only gets 1280×800 regardless of monitor size, and on larger
 *   monitors the app does not fill the screen. The BorderPane + StackPane layout
 *   is then fully responsive because the OS drives the window size.
 */
public class LoginController {

    @FXML private TextField     fldUsername;
    @FXML private PasswordField fldPassword;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;
    @FXML private StackPane     rootPane;
    @FXML private Label         lblDbStatus;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        FxUtils.animateIn(rootPane, 0);
        lblError.setVisible(false);

        // DB status ping on background thread
        new Thread(() -> {
            String status = DBConnection.statusLabel();
            javafx.application.Platform.runLater(() -> lblDbStatus.setText(status));
        }, "db-check").start();

        // Enter on username → move focus to password
        fldUsername.setOnAction(e -> fldPassword.requestFocus());
        // Enter on password → attempt login
        fldPassword.setOnAction(e -> onLogin());
    }

    @FXML
    private void onLogin() {
        String user = fldUsername.getText().trim();
        String pass = fldPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please enter username and password.");
            FxUtils.shake(rootPane);
            return;
        }

        if (userService.authenticate(user, pass)) {
            loadMainApp();
        } else {
            showError("Invalid username or password.");
            FxUtils.shake(rootPane);
            fldPassword.clear();
            fldPassword.requestFocus();
        }
    }

    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();

            // FIX: Use 1280×800 as the initial Scene size (used when not maximised).
            // Then call setMaximized(true) so the window fills the monitor.
            // This is critical for a dashboard app — without it BorderPane only
            // gets 1280×800 even on a 1920×1080 monitor.
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("ChequePro — Cheque & Invoice Manager");
            stage.setMinWidth(1000);
            stage.setMinHeight(600);

            // FIX: maximise the window so BorderPane fills the full screen.
            // All layout containers are responsive (using grow constraints) so
            // they will correctly fill whatever size the OS gives.
            stage.setMaximized(true);
            stage.centerOnScreen();

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(350), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            showError("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblError.setText("⚠  " + msg);
        lblError.setVisible(true);
    }
}
