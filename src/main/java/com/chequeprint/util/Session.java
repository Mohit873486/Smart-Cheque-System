package com.chequeprint.util;

/**
 * Session utility class for storing active user authentication details in JavaFX.
 * Stores JWT authentication token and username for REST API authorization.
 */
public class Session {

    // Public static fields for direct access: Session.token and Session.username
    public static String token;
    public static String username;

    private Session() {
        // Utility class
    }

    /**
     * Sets the active session context after successful login.
     *
     * @param token    The JWT token received from backend API
     * @param username The authenticated username
     */
    public static void setSession(String token, String username) {
        Session.token = token;
        Session.username = username;
        SessionManager.getInstance().setToken(token);
    }

    /**
     * Gets the active session JWT token.
     *
     * @return token string or empty string if not logged in
     */
    public static String getToken() {
        if (token != null && !token.isBlank()) {
            return token;
        }
        return SessionManager.getInstance().getToken();
    }

    /**
     * Gets the active session username.
     *
     * @return username string
     */
    public static String getUsername() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return SessionManager.getInstance().currentUser()
                .map(u -> u.getUsername())
                .orElse("");
    }

    /**
     * Convenience helper to generate HTTP Authorization header value.
     * Format: "Bearer <token>"
     *
     * @return Authorization header value string
     */
    public static String getAuthorizationHeader() {
        String activeToken = getToken();
        if (activeToken != null && !activeToken.isBlank()) {
            return "Bearer " + activeToken;
        }
        return "";
    }

    /**
     * Clears session data upon logout or session expiration.
     */
    public static void clear() {
        Session.token = null;
        Session.username = null;
        SessionManager.getInstance().clear();
    }
}
