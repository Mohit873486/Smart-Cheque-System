package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.service.AuthService;
import com.chequeprint.service.AuthenticationResult;
import com.chequeprint.service.ForgotPasswordService;
import com.chequeprint.util.DBConnection;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;
import java.util.prefs.Preferences;

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
  private Hyperlink linkForgotPassword;
  @FXML
  private Hyperlink linkOtpLogin;
  @FXML
  private ProgressIndicator loginProgress;
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
    hideError();

    boolean remembered = PREFS.getBoolean("login.remember", false);
    fldUsername.setText(PREFS.get("login.username", ""));
    chkRememberMe.setSelected(remembered);

    if (remembered && !fldUsername.getText().isBlank()) {
      Platform.runLater(fldPassword::requestFocus);
    }

    fldPasswordVisible.managedProperty().bind(fldPasswordVisible.visibleProperty());
    fldPasswordVisible.visibleProperty().addListener((obs, oldValue, newValue) -> {
      fldPassword.setVisible(!newValue);
    });
    fldPasswordVisible.textProperty().bindBidirectional(fldPassword.textProperty());
    btnTogglePassword.setText("Show");

    updateAttemptsLabel();
    setLoading(false);

    new Thread(() -> {
      String status = DBConnection.statusLabel();
      Platform.runLater(() -> lblDbStatus.setText(status));
    }, "login-db-status").start();

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
  private void onLogin() {
    String identity = fldUsername.getText() == null ? "" : fldUsername.getText().trim();
    String password = fldPassword.getText();

    if (identity.isEmpty() || password == null || password.isBlank()) {
      showError("Please enter username/email and password.");
      FxUtils.shake(rootPane);
      return;
    }

    setLoading(true);
    Task<AuthenticationResult> task = new Task<>() {
      @Override
      protected AuthenticationResult call() {
        return authService.authenticate(identity, password);
      }
    };

    task.setOnSucceeded(e -> {
      setLoading(false);
      AuthenticationResult result = task.getValue();
      if (result.success()) {
        persistRememberMe(identity);
        loadMainApp(result.user());
      } else {
        updateAttemptsLabel();
        showError(result.message());
        FxUtils.shake(rootPane);
        fldPassword.clear();
        fldPassword.requestFocus();
      }
    });

    task.setOnFailed(e -> {
      setLoading(false);
      showError("Login failed. Please try again.");
      FxUtils.shake(rootPane);
    });

    Thread thread = new Thread(task, "login-authentication");
    thread.setDaemon(true);
    thread.start();
  }

  @FXML
  private void onForgotPassword() {
    Optional<String> identity = promptText("Forgot Password", "Enter your username or email");
    if (identity.isEmpty() || identity.get().isBlank()) {
      return;
    }

    try {
      String otp = forgotPasswordService.startReset(identity.get());
      if (otp == null) {
        showError("No account found for that username or email.");
        return;
      }

      showInfo("OTP Generated", "Email delivery is not configured yet. Development OTP: " + otp);
      Optional<PasswordResetRequest> reset = promptPasswordReset(identity.get());
      if (reset.isEmpty()) {
        return;
      }

      forgotPasswordService.resetPassword(identity.get(), reset.get().otp(), reset.get().newPassword());
      showInfo("Password Updated", "Your password was reset successfully. Please sign in again.");
    } catch (Exception ex) {
      showError(ex.getMessage());
    }
  }

  @FXML
  private void onOtpLogin() {
    Optional<String> identity = promptText("OTP Login", "Enter your username or email");
    if (identity.isEmpty() || identity.get().isBlank()) {
      return;
    }

    try {
      String otp = forgotPasswordService.startReset(identity.get());
      if (otp == null) {
        showError("No account found for that username or email.");
        return;
      }

      showInfo("OTP Generated", "Email delivery is not configured yet. Development OTP: " + otp);
      Optional<String> enteredOtp = promptText("OTP Login", "Enter the 6-digit OTP");
      if (enteredOtp.isEmpty()) {
        return;
      }

      User user = forgotPasswordService.verifyOtpLogin(identity.get(), enteredOtp.get());
      SessionManager.start(user);
      persistRememberMe(identity.get());
      loadMainApp(user);
    } catch (Exception ex) {
      showError(ex.getMessage());
      FxUtils.shake(rootPane);
    }
  }

  private void setLoading(boolean loading) {
    btnLogin.setDisable(loading);
    fldUsername.setDisable(loading);
    fldPassword.setDisable(loading);
    fldPasswordVisible.setDisable(loading);
    btnTogglePassword.setDisable(loading);
    chkRememberMe.setDisable(loading);
    linkForgotPassword.setDisable(loading);
    linkOtpLogin.setDisable(loading);
    loginProgress.setVisible(loading);
    loginProgress.setManaged(loading);
    btnLogin.setText(loading ? "Signing in..." : "Login securely");
  }

  private void updateAttemptsLabel() {
    if (authService.isLocked()) {
      lblAttempts.setText("Account blocked. Contact administrator.");
      btnLogin.setDisable(true);
    } else {
      lblAttempts.setText(authService.getRemainingLoginAttempts() + " attempts remaining");
    }
  }

  private void persistRememberMe(String identity) {
    if (chkRememberMe.isSelected()) {
      PREFS.put("login.username", identity);
      PREFS.putBoolean("login.remember", true);
    } else {
      PREFS.remove("login.username");
      PREFS.putBoolean("login.remember", false);
    }
  }

  private void loadMainApp(User authenticatedUser) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
      Parent root = loader.load();

      MainController mainController = loader.getController();
      mainController.setCurrentUser(authenticatedUser);

      Stage stage = (Stage) btnLogin.getScene().getWindow();
      Scene scene = new Scene(root, 1280, 800);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

      stage.setScene(scene);
      stage.setTitle("Smart Cheque Management System");
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
      showError("Failed to load dashboard: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private Optional<String> promptText(String title, String prompt) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(prompt);
    ButtonType submit = new ButtonType("Continue", ButtonType.OK.getButtonData());
    dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

    TextField field = new TextField();
    field.setPromptText(prompt);
    field.setMaxWidth(Double.MAX_VALUE);
    dialog.getDialogPane().setContent(field);
    Platform.runLater(field::requestFocus);
    dialog.setResultConverter(button -> button == submit ? field.getText() : null);
    return dialog.showAndWait();
  }

  private Optional<PasswordResetRequest> promptPasswordReset(String identity) {
    Dialog<PasswordResetRequest> dialog = new Dialog<>();
    dialog.setTitle("Reset Password");
    dialog.setHeaderText("Reset password for " + identity);
    ButtonType submit = new ButtonType("Reset Password", ButtonType.OK.getButtonData());
    dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

    TextField otp = new TextField();
    otp.setPromptText("6-digit OTP");
    PasswordField newPassword = new PasswordField();
    newPassword.setPromptText("New password");

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.add(new Label("OTP"), 0, 0);
    grid.add(otp, 1, 0);
    grid.add(new Label("New Password"), 0, 1);
    grid.add(newPassword, 1, 1);

    dialog.getDialogPane().setContent(grid);
    Platform.runLater(otp::requestFocus);
    dialog.setResultConverter(button -> button == submit
        ? new PasswordResetRequest(otp.getText(), newPassword.getText())
        : null);
    return dialog.showAndWait();
  }

  private void showInfo(String title, String message) {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(message);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
    dialog.showAndWait();
  }

  private void showError(String msg) {
    lblError.setText(msg);
    lblError.setVisible(true);
    lblError.setManaged(true);
  }

  private void hideError() {
    lblError.setText("");
    lblError.setVisible(false);
    lblError.setManaged(false);
  }

  private record PasswordResetRequest(String otp, String newPassword) {
  }
}
