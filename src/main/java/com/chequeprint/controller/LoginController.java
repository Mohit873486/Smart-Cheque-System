package com.chequeprint.controller;

import com.chequeprint.service.AuthService;
import com.chequeprint.service.AuthenticationResult;
import com.chequeprint.service.ForgotPasswordService;
import com.chequeprint.util.DBConnection;
import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.prefs.Preferences;

/**
 * LoginController — credentials screen shown before main app.
 */
public class LoginController {
  private static final Preferences PREFS = Preferences.userNodeForPackage(LoginController.class);

  @FXML
  private TextField fldUsername;
  @FXML
  private PasswordField fldPassword;
  @FXML
  private TextField fldPasswordVisible;
  @FXML
  private CheckBox chkRememberMe;
  @FXML
  private Button btnLogin;
  @FXML
  private Button btnTogglePassword;
  @FXML
  private Label lblError;
  @FXML
  private Label lblAttempts;
  @FXML
  private Label lblDbStatus;
  @FXML
  private StackPane rootPane;

  private final AuthService authService = new AuthService();
  private final ForgotPasswordService forgotPasswordService = new ForgotPasswordService();

  @FXML
  public void initialize() {
    FxUtils.animateIn(rootPane, 0);
    lblError.setVisible(false);

    String savedUsername = PREFS.get("login.username", "");
    boolean remembered = PREFS.getBoolean("login.remember", false);
    fldUsername.setText(savedUsername);
    chkRememberMe.setSelected(remembered);

    if (remembered && !savedUsername.isBlank()) {
      fldPassword.requestFocus();
    }

    fldPasswordVisible.managedProperty().bind(fldPasswordVisible.visibleProperty());
    fldPasswordVisible.visibleProperty().addListener((obs, oldValue, newValue) -> {
      fldPassword.setVisible(!newValue);
    });
    fldPasswordVisible.textProperty().bindBidirectional(fldPassword.textProperty());
    btnTogglePassword.setText("Show");

    updateAttemptsLabel();

    // DB status ping on background thread
    new Thread(() -> {
      String status = DBConnection.statusLabel();
      javafx.application.Platform.runLater(() -> lblDbStatus.setText(status));
    }, "db-check").start();

    // Enter on username → move focus to password
    fldUsername.setOnAction(e -> fldPassword.requestFocus());
    fldPassword.setOnAction(e -> onLogin());
    fldPasswordVisible.setOnAction(e -> onLogin());
  }

  @FXML
  private void onTogglePassword() {
    boolean show = !fldPasswordVisible.isVisible();
    fldPasswordVisible.setVisible(show);
    btnTogglePassword.setText(show ? "Hide" : "Show");
    if (show) {
      fldPasswordVisible.requestFocus();
      fldPasswordVisible.end();
    } else {
      fldPassword.requestFocus();
      fldPassword.end();
    }
  }

  @FXML
  private void onForgotPassword() {
    String identity = fldUsername.getText() == null ? "" : fldUsername.getText().trim();
    if (identity.isBlank()) {
      showError("Enter your username or email first, then click Forgot password.");
      fldUsername.requestFocus();
      return;
    }

    try {
      String otp = forgotPasswordService.startReset(identity);
      Alert otpAlert = new Alert(Alert.AlertType.INFORMATION);
      otpAlert.setTitle("Password Reset OTP");
      otpAlert.setHeaderText("OTP generated for the registered account.");
      otpAlert.setContentText(otp == null
          ? "If the account exists, an OTP has been sent to the registered contact."
          : "Development mode OTP: " + otp + "\n\nIn production, send this by email/SMS and never display it here.");
      otpAlert.showAndWait();

      Dialog<ButtonType> dialog = new Dialog<>();
      dialog.setTitle("Reset Password");
      dialog.setHeaderText("Verify OTP and set a new password");

      TextField otpField = new TextField();
      otpField.setPromptText("6-digit OTP");
      PasswordField newPassword = new PasswordField();
      newPassword.setPromptText("New password");
      PasswordField confirmPassword = new PasswordField();
      confirmPassword.setPromptText("Confirm new password");
      VBox content = new VBox(10, new Label("OTP"), otpField, new Label("New password"), newPassword,
          new Label("Confirm password"), confirmPassword);
      content.setPrefWidth(360);
      dialog.getDialogPane().setContent(content);
      dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      dialog.showAndWait().ifPresent(type -> {
        if (type != ButtonType.OK) {
          return;
        }
        try {
          if (!newPassword.getText().equals(confirmPassword.getText())) {
            throw new IllegalArgumentException("Passwords do not match.");
          }
          forgotPasswordService.resetPassword(identity, otpField.getText(), newPassword.getText());
          showError("Password reset successful. Please login with the new password.");
        } catch (Exception e) {
          showError(e.getMessage());
          FxUtils.shake(rootPane);
        }
      });
    } catch (Exception e) {
      showError(e.getMessage());
      FxUtils.shake(rootPane);
    }
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

    AuthenticationResult result = authService.authenticate(user, pass);
    if (result.success()) {
      if (chkRememberMe.isSelected()) {
        PREFS.put("login.username", user);
        PREFS.putBoolean("login.remember", true);
      } else {
        PREFS.remove("login.username");
        PREFS.putBoolean("login.remember", false);
      }
      loadMainApp(result.user());
    } else {
      updateAttemptsLabel();
      showError(result.message());
      FxUtils.shake(rootPane);
      fldPassword.clear();
      fldPassword.requestFocus();
    }
  }

  private void updateAttemptsLabel() {
    lblAttempts.setText("Account locks after 3 failed attempts");
    btnLogin.setDisable(false);
  }

  private void loadMainApp(com.chequeprint.model.User authenticatedUser) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
      Parent root = loader.load();

      com.chequeprint.controller.MainController mainController = loader.getController();
      mainController.setCurrentUser(authenticatedUser);

      Stage stage = (Stage) btnLogin.getScene().getWindow();
      Scene scene = new Scene(root, 1280, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

      stage.setScene(scene);
      stage.setTitle("ChequePro — Cheque & Invoice Manager");
      stage.setMinWidth(1000);
      stage.setMinHeight(600);
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
