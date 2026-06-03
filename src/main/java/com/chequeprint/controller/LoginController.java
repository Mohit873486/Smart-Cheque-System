package com.chequeprint.controller;

import com.chequeprint.service.AuthService;
import com.chequeprint.service.AuthenticationResult;
import com.chequeprint.util.DBConnection;
import com.chequeprint.util.FxUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
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
  private ChoiceBox<String> choiceRole;
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

  @FXML
  public void initialize() {
    FxUtils.animateIn(rootPane, 0);
    lblError.setVisible(false);

    String savedUsername = PREFS.get("login.username", "");
    boolean remembered = PREFS.getBoolean("login.remember", false);
    String savedRole = PREFS.get("login.role", "Admin");
    fldUsername.setText(savedUsername);
    choiceRole.getItems().setAll("Admin", "Manager", "Operator", "Auditor");
    choiceRole.setValue(savedRole);
    chkRememberMe.setSelected(remembered);

    if (remembered && !savedUsername.isBlank()) {
      fldPassword.requestFocus();
    }

    choiceRole.getItems().setAll("Admin", "Manager", "Operator", "Auditor");
    if (choiceRole.getValue() == null) {
      choiceRole.setValue("Admin");
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
    showError("Forgot password? Contact your administrator or use your registered email to reset.");
  }

  @FXML
  private void onLogin() {
    String user = fldUsername.getText().trim();
    String pass = fldPassword.getText();
    String role = choiceRole.getValue();

    if (user.isEmpty() || pass.isEmpty()) {
      showError("Please enter username and password.");
      FxUtils.shake(rootPane);
      return;
    }

    AuthenticationResult result = authService.authenticate(user, pass, role);
    if (result.success()) {
      if (chkRememberMe.isSelected()) {
        PREFS.put("login.username", user);
        PREFS.put("login.role", role);
        PREFS.putBoolean("login.remember", true);
      } else {
        PREFS.remove("login.username");
        PREFS.remove("login.role");
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
    if (authService.isLocked()) {
      lblAttempts.setText("Account locked. Contact administrator.");
      btnLogin.setDisable(true);
    } else {
      lblAttempts.setText(authService.getRemainingLoginAttempts() + " attempts remaining");
      btnLogin.setDisable(false);
    }
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
