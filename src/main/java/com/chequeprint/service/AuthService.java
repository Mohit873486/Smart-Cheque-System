package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.util.PasswordUtil;
import com.chequeprint.util.SessionManager;

import java.sql.SQLException;

public class AuthService {

  private final UserDAO dao = new UserDAO();
  private static final int MAX_LOGIN_ATTEMPTS = 3;
  private int remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
  private User currentUser;

  public AuthenticationResult authenticate(String usernameOrEmail, String password) {
    if (usernameOrEmail == null || usernameOrEmail.isBlank()
        || password == null || password.isBlank()) {
      return AuthenticationResult.failure("Username/email and password are required.");
    }

    try {
      User user = dao.findByUsernameOrEmail(usernameOrEmail.trim());
      if (user == null) {
        return AuthenticationResult.failure("Invalid user. Please check your username or email.");
      }

      if (isBlocked(user)) {
        remainingLoginAttempts = 0;
        return AuthenticationResult.failure("Blocked account. Contact an administrator to unlock it.");
      }

      if (!PasswordUtil.matches(password, user.getPassword())) {
        int attempts = dao.incrementFailedLoginAttempts(user.getId());
        remainingLoginAttempts = Math.max(0, MAX_LOGIN_ATTEMPTS - attempts);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
          dao.lockUser(user.getId());
          return AuthenticationResult.failure("Blocked account. Maximum login attempts reached.");
        }
        return AuthenticationResult.failure("Wrong password. " + remainingLoginAttempts + " attempt(s) remaining.");
      }

      dao.resetLoginAttempts(user.getId());
      resetAttempts();
      currentUser = user;
      SessionManager.start(user);
      return AuthenticationResult.success(user);
    } catch (SQLException e) {
      return AuthenticationResult.failure("Login failed: " + e.getMessage());
    }
  }

  public AuthenticationResult authenticate(String usernameOrEmail, String password, String ignoredRoleName) {
    return authenticate(usernameOrEmail, password);
  }

  private boolean isBlocked(User user) {
    String status = user.getStatus();
    return user.isAccountLocked()
        || user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS
        || "Locked".equalsIgnoreCase(status)
        || "Disabled".equalsIgnoreCase(status);
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
    remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
    SessionManager.clear();
  }

  public String getLandingPage() {
    if (currentUser == null) {
      return "dashboard";
    }
    return switch (UserRole.from(currentUser.getRole())) {
      case ADMIN -> "dashboard";
      case USER -> "cheques";
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
      case USER -> page.equals("dashboard") || page.equals("cheques") || page.equals("profile")
          || page.equals("support");
      case MANAGER ->
        page.equals("dashboard") || page.equals("cheques") || page.equals("profile") || page.equals("support");
      case OPERATOR -> page.equals("dashboard") || page.equals("cheques") || page.equals("ai") || page.equals("profile")
          || page.equals("support");
      case AUDITOR ->
        page.equals("dashboard") || page.equals("invoices") || page.equals("profile") || page.equals("support");
    };
  }

  private void resetAttempts() {
    remainingLoginAttempts = MAX_LOGIN_ATTEMPTS;
  }
}
