package com.chequeprint.model;

public enum UserRole {
  ADMIN("Admin"),
  MANAGER("Manager"),
  OPERATOR("Operator"),
  AUDITOR("Auditor");

  private final String label;

  UserRole(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static UserRole from(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Role cannot be null or empty");
    }
    String normalized = value.trim();
    for (UserRole role : values()) {
      if (role.label.equalsIgnoreCase(normalized) || role.name().equalsIgnoreCase(normalized)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown role: " + value);
  }
}
