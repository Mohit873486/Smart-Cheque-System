package com.chequeprint.util;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;

import java.time.Instant;
import java.util.Optional;

public final class SessionManager {
    private static User currentUser;
    private static String jwtToken;
    private static Instant loginTime;
    private static Instant lastActivityTime;
    private static final long SESSION_TIMEOUT_SECONDS = 900; // 15 minutes

    private SessionManager() {
    }

    public static void start(User user) {
        currentUser = user;
        loginTime = Instant.now();
        lastActivityTime = Instant.now();
    }

    public static void setToken(String token) {
        jwtToken = token;
    }

    public static String getToken() {
        return jwtToken;
    }

    public static void setJwtToken(String token) {
        setToken(token);
    }

    public static String getJwtToken() {
        return getToken();
    }

    public static Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static User requireUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("No authenticated user in session."));
    }

    public static void updateActivity() {
        lastActivityTime = Instant.now();
    }

    public static boolean isExpired() {
        if (currentUser == null) {
            return false;
        }
        return lastActivityTime != null && Instant.now().isAfter(lastActivityTime.plusSeconds(SESSION_TIMEOUT_SECONDS));
    }

    public static boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRoleEnum() == role;
    }

    public static boolean hasPermission(Permission permission) {
        return AccessControl.can(currentUser, permission);
    }

    public static Instant loginTime() {
        return loginTime;
    }

    public static void clear() {
        currentUser = null;
        jwtToken = null;
        loginTime = null;
        lastActivityTime = null;
    }
}
