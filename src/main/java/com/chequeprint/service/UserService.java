package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;

import java.sql.SQLException;

import java.util.Objects;

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

    // ── Admin user management helpers ─────────────────────────────────
    public java.util.List<User> findAllUsers(User actor) throws SQLException {
        // Caller is expected to have performed permission checks; keep service focused
        return dao.findAll();
    }

    public void createUser(User actor, String username, String name, String email, String password, UserRole role)
            throws SQLException {
        User u = new User();
        u.setUsername(username);
        u.setName(name);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role == null ? UserRole.OPERATOR.label() : role.label());
        validateUser(u);
        dao.insertOrUpdate(u);
    }

    public void updateUser(User actor, User target, String newPassword) throws SQLException {
        if (target == null)
            throw new IllegalArgumentException("User cannot be null");
        validateUser(target);
        // Only set password when provided
        if (newPassword == null || newPassword.isBlank()) {
            target.setPassword(null);
        } else {
            target.setPassword(newPassword);
        }
        dao.insertOrUpdate(target);
    }

    public void deleteUser(User actor, int userId) throws SQLException {
        dao.deleteById(userId);
    }

}