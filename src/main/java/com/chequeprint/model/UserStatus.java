package com.chequeprint.model;

public enum UserStatus {
  Active("Active"),
  Disabled("Disabled"),
  Locked("Locked");

  private final String label;

  UserStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static UserStatus from(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Status cannot be null or empty");
    }
    String normalized = value.trim();
    for (UserStatus status : values()) {
      if (status.label.equalsIgnoreCase(normalized) || status.name().equalsIgnoreCase(normalized)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + value);
  }
}
