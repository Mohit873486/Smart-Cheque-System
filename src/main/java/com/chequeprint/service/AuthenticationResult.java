package com.chequeprint.service;

import com.chequeprint.model.User;

public record AuthenticationResult(boolean success, String message, User user) {

  public static AuthenticationResult success(User user) {
    return new AuthenticationResult(true, "Login successful", user);
  }

  public static AuthenticationResult failure(String message) {
    return new AuthenticationResult(false, message, null);
  }
}
