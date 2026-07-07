package com.chequeprint.controller;

import com.chequeprint.model.UserRole;
import com.chequeprint.model.User;
import com.chequeprint.service.UserService;
import com.chequeprint.util.SessionManager;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignupController {
  @FXML
  TextField fldUsername;
  @FXML
  PasswordField fldPassword;
  @FXML
  PasswordField fldConfirm;
  @FXML
  ComboBox<UserRole> cmbRole;
  @FXML
  Hyperlink linkLogin;
  @FXML
  Label errUsername, errPassword, errConfirm, errRole, lblStatus;

  private final UserService userService = new UserService();

  @FXML
  public void initialize() {
    cmbRole.getItems().setAll(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR, UserRole.AUDITOR, UserRole.USER);
    cmbRole.setValue(UserRole.OPERATOR);
    // Only show role selector to Admin users (if session exists)
    var actor = SessionManager.getInstance().currentUser().orElse(null);
    cmbRole.setVisible(actor != null && AccessControl.can(actor, Permission.MANAGE_USERS));
  }

  @FXML
  private void onCreateAccount() {
    clearErrors();
    String username = fldUsername.getText().trim();
    String pwd = fldPassword.getText();
    String confirm = fldConfirm.getText();
    UserRole selectedRole = cmbRole.getValue();

    boolean ok = true;
    if (username.isBlank()) {
      errUsername.setText("Username required");
      ok = false;
    }
    if (pwd == null || pwd.length() < 6) {
      errPassword.setText("Minimum 6 characters");
      ok = false;
    }
    if (!pwd.equals(confirm)) {
      errConfirm.setText("Passwords do not match");
      ok = false;
    }

    if (!ok)
      return;

    // Prevent self-assigning ADMIN when no admin actor
    var actor = SessionManager.getInstance().currentUser().orElse(null);
    if ((actor == null || !AccessControl.can(actor, Permission.MANAGE_USERS)) && selectedRole == UserRole.ADMIN) {
      errRole.setText("Cannot self-assign ADMIN");
      return;
    }

    try {
      userService.registerUser(username, pwd, selectedRole, actor);
      lblStatus.setText("Account created — proceed to login");
      // optionally navigate to login: mainController.navigate("login")
    } catch (Exception ex) {
      errUsername.setText(ex.getMessage());
    }
  }

  private void clearErrors() {
    errUsername.setText("");
    errPassword.setText("");
    errConfirm.setText("");
    errRole.setText("");
    lblStatus.setText("");
  }

  @FXML
  private void onBackToLogin() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
      Parent root = loader.load();
      Stage stage = (Stage) fldUsername.getScene().getWindow();
      Scene scene = new Scene(root, 1040, 620);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      stage.setScene(scene);
      stage.centerOnScreen();
    } catch (Exception e) {
      lblStatus.setText("Unable to navigate to login.");
      e.printStackTrace();
    }
  }
}