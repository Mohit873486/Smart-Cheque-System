package com.chequeprint.util;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;

import java.time.Instant;
import java.util.Optional;

public final class SessionManager {
    private static User currentUser;
    private static Instant loginTime;

    private SessionManager() {
    }

    public static void start(User user) {
        currentUser = user;
        loginTime = Instant.now();
    }

    public static Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static User requireUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("No authenticated user in session."));
    }

    public static boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRoleEnum() == role;
    }

    public static Instant loginTime() {
        return loginTime;
    }

    public static void clear() {
        currentUser = null;
        loginTime = null;
    }
}
