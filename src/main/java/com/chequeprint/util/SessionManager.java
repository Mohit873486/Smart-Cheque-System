package com.chequeprint.util;

import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;

import java.time.Instant;
import java.util.Optional;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private String jwtToken;
    private Instant loginTime;
    private Instant lastActivityTime;
    private final long SESSION_TIMEOUT_SECONDS = 900; // 15 minutes

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void start(User user) {
        this.currentUser = user;
        this.loginTime = Instant.now();
        this.lastActivityTime = Instant.now();
    }

    public void setToken(String token) {
        this.jwtToken = token;
    }

    public String getToken() {
        return this.jwtToken;
    }

    public void setJwtToken(String token) {
        setToken(token);
    }

    public String getJwtToken() {
        return getToken();
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public User requireUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("No authenticated user in session."));
    }

    public void updateActivity() {
        this.lastActivityTime = Instant.now();
    }

    public boolean isExpired() {
        if (currentUser == null) {
            return false;
        }
        return lastActivityTime != null && Instant.now().isAfter(lastActivityTime.plusSeconds(SESSION_TIMEOUT_SECONDS));
    }

    public boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRoleEnum() == role;
    }

    public boolean hasPermission(Permission permission) {
        return AccessControl.can(currentUser, permission);
    }

    public Instant loginTime() {
        return loginTime;
    }

    public void clear() {
        this.currentUser = null;
        this.jwtToken = null;
        this.loginTime = null;
        this.lastActivityTime = null;
    }
}
