package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;

import java.sql.SQLException;

public class AuthService {

  private final UserDAO dao = new UserDAO();
  private final int maxLoginAttempts = 3;
  private int remainingLoginAttempts = maxLoginAttempts;
  private User currentUser;

  public AuthenticationResult authenticate(String usernameOrEmail, String password, String roleName) {
    if (isLocked()) {
      return AuthenticationResult.failure("Account locked due to too many failed login attempts.");
    }

    if (usernameOrEmail == null || usernameOrEmail.isBlank()
        || password == null || password.isBlank()
        || roleName == null || roleName.isBlank()) {
      decrementAttempts();
      return AuthenticationResult.failure("Username, password, and role are required.");
    }

    UserRole role;
    try {
      role = UserRole.from(roleName);
    } catch (IllegalArgumentException e) {
      decrementAttempts();
      return AuthenticationResult.failure("Selected role is invalid.");
    }

    try {
      User user = dao.findByUsernameOrEmailAndRole(usernameOrEmail.trim(), role.label());
      if (user == null) {
        decrementAttempts();
        return AuthenticationResult.failure("Invalid username, password, or role.");
      }
      if (!password.equals(user.getPassword())) {
        decrementAttempts();
        return AuthenticationResult.failure("Invalid username, password, or role.");
      }
      resetAttempts();
      currentUser = user;
      return AuthenticationResult.success(user);
    } catch (SQLException e) {
      decrementAttempts();
      return AuthenticationResult.failure("Login failed: " + e.getMessage());
    }
  }

  public boolean isLocked() {
    return remainingLoginAttempts <= 0;
  }

  public int getRemainingLoginAttempts() {
    return remainingLoginAttempts;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void logout() {
    currentUser = null;
    remainingLoginAttempts = maxLoginAttempts;
  }

  public String getLandingPage() {
    if (currentUser == null) {
      return "dashboard";
    }
    return switch (UserRole.from(currentUser.getRole())) {
      case ADMIN -> "dashboard";
      case MANAGER -> "cheques";
      case OPERATOR -> "cheques";
      case AUDITOR -> "invoices";
    };
  }

  public boolean canAccessPage(String page) {
    if (currentUser == null) {
      return false;
    }
    UserRole role = UserRole.from(currentUser.getRole());
    return switch (role) {
      case ADMIN -> true;
      case MANAGER ->
        page.equals("dashboard") || page.equals("cheques") || page.equals("profile") || page.equals("support");
      case OPERATOR -> page.equals("dashboard") || page.equals("cheques") || page.equals("ai") || page.equals("profile")
          || page.equals("support");
      case AUDITOR ->
        page.equals("dashboard") || page.equals("invoices") || page.equals("profile") || page.equals("support");
    };
  }

  private void decrementAttempts() {
    if (remainingLoginAttempts > 0) {
      remainingLoginAttempts--;
    }
  }

  private void resetAttempts() {
    remainingLoginAttempts = maxLoginAttempts;
  }
}
