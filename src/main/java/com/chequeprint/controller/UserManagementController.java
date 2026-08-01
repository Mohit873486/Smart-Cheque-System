package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;
import com.chequeprint.service.UserService;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class UserManagementController {

    @FXML
    private VBox rootPane;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private Label lblFormTitle;
    @FXML
    private TextField fldUsername;
    @FXML
    private TextField fldName;
    @FXML
    private TextField fldEmail;
    @FXML
    private PasswordField fldPassword;
    @FXML
    private ComboBox<UserRole> cmbRole;

    private final UserService userService = new UserService();
    private MainController mainController;
    private User selectedUser;

    @FXML
    public void initialize() {
        configureTable();
        cmbRole.setItems(FXCollections.observableArrayList(
                UserRole.ADMIN, UserRole.USER, UserRole.MANAGER, UserRole.OPERATOR, UserRole.AUDITOR));
        cmbRole.setValue(UserRole.OPERATOR);
        loadUsers();
        FxUtils.animateIn(rootPane, 0);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void configureTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == null ? "Active" : c.getValue().getStatus()));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> {
            if (user != null) {
                populateForm(user);
            }
        });
    }

    private void loadUsers() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.MANAGE_USERS)) {
            showAlert("Permission Denied", "You do not have permission to manage users.", Alert.AlertType.ERROR);
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                var users = userService.findAllUsers(actor);
                Platform.runLater(() -> userTable.getItems().setAll(users));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Load Error", e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "load-users");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onSave() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.MANAGE_USERS)) {
            showAlert("Permission Denied", "You do not have permission to manage users.", Alert.AlertType.ERROR);
            return;
        }

        String username = fldUsername.getText();
        String name = fldName.getText();
        String email = fldEmail.getText();
        String pwd = fldPassword.getText();
        UserRole role = cmbRole.getValue();

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (selectedUser == null) {
                    userService.createUser(actor, username, name, email, pwd, role);
                } else {
                    selectedUser.setUsername(username);
                    selectedUser.setName(name);
                    selectedUser.setEmail(email);
                    selectedUser.setRole(role.label());
                    userService.updateUser(actor, selectedUser, pwd);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(evt -> {
            showAlert("Success", selectedUser == null ? "User created successfully." : "User updated successfully.", Alert.AlertType.INFORMATION);
            clearForm();
            loadUsers();
        });

        saveTask.setOnFailed(evt -> {
            Throwable ex = saveTask.getException();
            showAlert("User Error", ex != null ? ex.getMessage() : "User operation failed.", Alert.AlertType.ERROR);
        });

        Thread t = new Thread(saveTask, "user-save");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onDelete() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.MANAGE_USERS)) {
            showAlert("Permission Denied", "You do not have permission to manage users.", Alert.AlertType.ERROR);
            return;
        }

        User user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            showAlert("No Selection", "Please select a user to delete.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user " + user.getUsername() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        userService.deleteUser(actor, user.getId());
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(evt -> {
                    clearForm();
                    loadUsers();
                });

                deleteTask.setOnFailed(evt -> {
                    Throwable ex = deleteTask.getException();
                    showAlert("Delete Error", ex != null ? ex.getMessage() : "Delete failed.", Alert.AlertType.ERROR);
                });

                Thread t = new Thread(deleteTask, "user-delete");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void populateForm(User user) {
        selectedUser = user;
        lblFormTitle.setText("Edit User");
        fldUsername.setText(user.getUsername());
        fldName.setText(user.getName());
        fldEmail.setText(user.getEmail());
        fldPassword.clear();
        fldPassword.setPromptText("Leave blank to keep current password");
        try {
            cmbRole.setValue(UserRole.from(user.getRole()));
        } catch (Exception ex) {
            // Fallback to OPERATOR if stored role is missing or unrecognized
            cmbRole.setValue(UserRole.OPERATOR);
        }
    }

    private void clearForm() {
        selectedUser = null;
        lblFormTitle.setText("New User");
        fldUsername.clear();
        fldName.clear();
        fldEmail.clear();
        fldPassword.clear();
        fldPassword.setPromptText("Required for new user");
        cmbRole.setValue(UserRole.OPERATOR);
        userTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
