package com.chequeprint.service;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.util.AccessDeniedException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class AccessControl {

  private static final Map<UserRole, Set<Permission>> GRANTS = new EnumMap<>(UserRole.class);

  static {
    GRANTS.put(UserRole.ADMIN, EnumSet.of(
        Permission.VIEW_DASHBOARD,
        Permission.VIEW_CHEQUES,
        Permission.CREATE_CHEQUE,
        Permission.UPDATE_CHEQUE,
        Permission.DELETE_CHEQUE,
        Permission.SUBMIT_CHEQUE,
        Permission.APPROVE_CHEQUE,
        Permission.REJECT_CHEQUE,
        Permission.PRINT_CHEQUE,
        Permission.VIEW_INVOICES,
        Permission.VIEW_BANK_TEMPLATES,
        Permission.ACCESS_AI_ASSISTANT,
        Permission.VIEW_SUPPORT,
        Permission.VIEW_PROFILE,
        Permission.UPDATE_PROFILE,
        Permission.MANAGE_SETTINGS,
        Permission.MANAGE_USERS,
        Permission.VIEW_AUDIT_LOG));

    GRANTS.put(UserRole.MANAGER, EnumSet.of(
        Permission.VIEW_DASHBOARD,
        Permission.VIEW_CHEQUES,
        Permission.APPROVE_CHEQUE,
        Permission.REJECT_CHEQUE,
        Permission.VIEW_INVOICES,
        Permission.VIEW_SUPPORT,
        Permission.VIEW_PROFILE,
        Permission.UPDATE_PROFILE));

    GRANTS.put(UserRole.OPERATOR, EnumSet.of(
        Permission.VIEW_DASHBOARD,
        Permission.VIEW_CHEQUES,
        Permission.CREATE_CHEQUE,
        Permission.UPDATE_CHEQUE,
        Permission.SUBMIT_CHEQUE,
        Permission.PRINT_CHEQUE,
        Permission.VIEW_SUPPORT,
        Permission.VIEW_PROFILE,
        Permission.UPDATE_PROFILE));

    GRANTS.put(UserRole.AUDITOR, EnumSet.of(
        Permission.VIEW_DASHBOARD,
        Permission.VIEW_INVOICES,
        Permission.VIEW_BANK_TEMPLATES,
        Permission.VIEW_SUPPORT,
        Permission.VIEW_PROFILE,
        Permission.VIEW_AUDIT_LOG));
  }

  private AccessControl() {
  }

  public static boolean can(User user, Permission permission) {
    if (user == null || permission == null) {
      return false;
    }
    UserRole role = user.getRoleEnum();
    return GRANTS.getOrDefault(role, Set.of()).contains(permission);
  }

  public static void requirePermission(User user, Permission permission) {
    if (!can(user, permission)) {
      throw new AccessDeniedException("User does not have permission: " + permission);
    }
  }

  public static boolean canAccessPage(User user, String page) {
    if (user == null || page == null) {
      return false;
    }
    UserRole role = user.getRoleEnum();
    return switch (role) {
      case ADMIN -> true;
      case MANAGER -> page.equals("dashboard") || page.equals("cheques") || page.equals("profile")
          || page.equals("support");
      case OPERATOR -> page.equals("dashboard") || page.equals("cheques") || page.equals("ai") || page.equals("profile")
          || page.equals("support");
      case AUDITOR -> page.equals("dashboard") || page.equals("invoices") || page.equals("profile")
          || page.equals("support");
    };
  }
}
