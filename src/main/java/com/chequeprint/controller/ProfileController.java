package com.chequeprint.controller;

import com.chequeprint.model.User;
import com.chequeprint.service.UserService;
import com.chequeprint.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * ProfileController — displays and saves the user profile.
 *
 * Fixes applied vs original:
 *  • Uses UserService (not UserDAO directly) — keeps service layer consistent.
 *  • Validation error shown as in-UI Alert, not swallowed to e.printStackTrace().
 *  • updateInitials() moved to UserService.getInitials() for reuse.
 */
public class ProfileController {

    @FXML private TextField fldName, fldEmail, fldPhone, fldCompany;
    @FXML private TextArea  fldAddress;
    @FXML private Label     lblInitials;
    @FXML private VBox      rootPane;

    private final UserService userService = new UserService();
    private User user;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        FxUtils.animateIn(rootPane, 0);
        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                user = userService.loadProfile();
                if (user != null) {
                    javafx.application.Platform.runLater(() -> {
                        fldName   .setText(user.getName()    != null ? user.getName()    : "");
                        fldEmail  .setText(user.getEmail()   != null ? user.getEmail()   : "");
                        fldPhone  .setText(user.getPhone()   != null ? user.getPhone()   : "");
                        fldCompany.setText(user.getCompany() != null ? user.getCompany() : "");
                        fldAddress.setText(user.getAddress() != null ? user.getAddress() : "");
                        lblInitials.setText(UserService.getInitials(user.getName()));
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR,
                        "Could not load profile: " + e.getMessage()).show());
            }
        }, "load-profile").start();
    }

    @FXML
    private void onSave() {
        try {
            if (user == null) user = new User();
            user.setName   (fldName   .getText().trim());
            user.setEmail  (fldEmail  .getText().trim());
            user.setPhone  (fldPhone  .getText().trim());
            user.setCompany(fldCompany.getText().trim());
            user.setAddress(fldAddress.getText().trim());

            userService.saveProfile(user);
            lblInitials.setText(UserService.getInitials(user.getName()));
            new Alert(Alert.AlertType.INFORMATION, "Profile saved successfully.").show();

        } catch (IllegalArgumentException iae) {
            FxUtils.shake(rootPane);
            new Alert(Alert.AlertType.WARNING, iae.getMessage()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage()).show();
        }
    }
}