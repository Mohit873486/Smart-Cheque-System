package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.model.UserStatus;
import com.chequeprint.service.AccessControl;
import com.chequeprint.util.PasswordUtil;
import com.chequeprint.util.SessionManager;

import java.sql.SQLException;

public class AuthService {

  private static final int MAX_LOGIN_ATTEMPTS = 3;

  private final UserDAO dao = new UserDAO();
  private final AuditService auditService = new AuditService();

  public AuthenticationResult authenticate(String usernameOrEmail, String password) {
    if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
      return AuthenticationResult.failure("Username/email and password are required.");
    }

    try {
      User user = dao.findByUsernameOrEmail(usernameOrEmail.trim());
      if (user == null) {
        return AuthenticationResult.failure("Invalid username/email or password.");
      }

      if (user.isAccountLocked() || user.getStatusEnum() == UserStatus.Locked) {
        return AuthenticationResult.failure("Account locked due to too many failed attempts or administrator action.");
      }
      if (user.getStatusEnum() != UserStatus.Active) {
        return AuthenticationResult.failure("Account is not active. Contact your administrator.");
      }

      if (!PasswordUtil.matches(password, user.getPassword())) {
        dao.recordFailedLogin(user.getId(), MAX_LOGIN_ATTEMPTS);
        int remaining = Math.max(0, MAX_LOGIN_ATTEMPTS - user.getFailedLoginAttempts() - 1);
        return AuthenticationResult.failure(remaining == 0
            ? "Account locked after 3 failed attempts."
            : "Invalid username/email or password. " + remaining + " attempts remaining.");
      }

      if (!PasswordUtil.isBcryptHash(user.getPassword())) {
        dao.updatePassword(user.getId(), PasswordUtil.hash(password));
        user = dao.findById(user.getId());
      } else {
        dao.resetLoginFailures(user.getId());
      }

      SessionManager.start(user);
      try {
        auditService.recordLogin(user);
      } catch (SQLException ignored) {
        // Audit failure should not block authentication
      }
      return AuthenticationResult.success(user);
    } catch (SQLException e) {
      return AuthenticationResult.failure("Login failed. Please check database connectivity.");
    }
  }

  public User getCurrentUser() {
    return SessionManager.currentUser().orElse(null);
  }

  public void logout() {
    User currentUser = getCurrentUser();
    try {
      auditService.recordLogout(currentUser);
    } catch (SQLException ignored) {
      // Audit failure should not block logout flow
    }
    SessionManager.clear();
  }

  public String getLandingPage() {
    User currentUser = getCurrentUser();
    if (currentUser == null) {
      return "dashboard";
    }
    return switch (UserRole.from(currentUser.getRole())) {
      case ADMIN -> "dashboard";
      case MANAGER, OPERATOR -> "cheques";
      case AUDITOR -> "invoices";
    };
  }

  public boolean canAccessPage(String page) {
    User currentUser = getCurrentUser();
    return AccessControl.canAccessPage(currentUser, page);
  }
}
