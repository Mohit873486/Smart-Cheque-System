package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;

import java.sql.SQLException;

/**
 * UserService — business logic for user profile management.
 *
 * Responsibilities:
 * • Input validation before DAO calls
 * • Password hashing placeholder (swap MD5 for BCrypt in production)
 * • Centralised credential check used by LoginController
 */
public class UserService {

    private final UserDAO dao = new UserDAO();

    // Hard-coded default credentials — replace with DB lookup in production
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASS = "admin123";

    // ── Authentication ───────────────────────────────────────────────
    /**
     * Returns true when the supplied credentials are valid.
     * Currently checks against hard-coded defaults; swap for DB lookup
     * once a proper users table with hashed passwords is in place.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null)
            return false;
        return username.trim().equals(DEFAULT_USER)
                && password.equals(DEFAULT_PASS);
    }

    // ── Profile CRUD ─────────────────────────────────────────────────
    /** Loads the first (and usually only) user profile from the database. */
    public User loadProfile() throws SQLException {
        return dao.findFirst();
    }

    /** Validates and saves (insert or update) a user profile. */
    public boolean saveProfile(User user) throws SQLException {
        validateUser(user);
        return dao.insertOrUpdate(user);
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private void validateUser(User user) {
        if (user == null)
            throw new IllegalArgumentException("User must not be null.");
        if (user.getName() == null || user.getName().isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && !user.getEmail().contains("@"))
            throw new IllegalArgumentException("Invalid email address.");
    }

    /**
     * Derives display initials from a full name.
     * e.g. "Raj Kumar" → "RK", "Admin" → "AD"
     */
    public static String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    /**
     * Changes the user's password after validating the current one.
     */
    public void changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        User user = dao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        // Verify current password
        if (!currentPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(newPassword);
        dao.insertOrUpdate(user);
    }

}