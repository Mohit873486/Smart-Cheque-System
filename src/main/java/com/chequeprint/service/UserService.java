package com.chequeprint.service;

import com.chequeprint.dao.UserDAO;
import com.chequeprint.model.User;
import com.chequeprint.model.UserRole;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.Permission;
import com.chequeprint.util.PasswordUtil;

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
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
        user.setPassword(PasswordUtil.hash(newPassword));
        dao.insertOrUpdate(user);
    }

    // ── Admin user management helpers ─────────────────────────────────
    public java.util.List<User> findAllUsers(User actor) throws SQLException {
        // Caller is expected to have performed permission checks; keep service focused
        return dao.findAll();
    }

    public boolean userExists(String usernameOrEmail) throws SQLException {
        return dao.findByUsernameOrEmail(usernameOrEmail) != null;
    }

    public void registerUser(String username, String password, UserRole role, User createdBy) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (userExists(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        User u = new User();
        u.setUsername(username);
        u.setName(username);
        u.setEmail(username.contains("@") ? username : username + "@example.com");
        u.setPassword(PasswordUtil.hash(password));
        u.setRole((createdBy != null && AccessControl.can(createdBy, Permission.MANAGE_USERS) && role != null)
                ? role.label()
                : UserRole.OPERATOR.label());
        u.setStatus("Active");
        validateUser(u);
        dao.insertOrUpdate(u);
    }

    public void createUser(User actor, String username, String name, String email, String password, UserRole role)
            throws SQLException {
        if (actor == null || !AccessControl.can(actor, Permission.MANAGE_USERS)) {
            throw new IllegalArgumentException("Only administrators can create users.");
        }
        if (userExists(username)) {
            throw new IllegalArgumentException("Username or email already exists.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        User u = new User();
        u.setUsername(username);
        u.setName(name == null || name.isBlank() ? username : name);
        u.setEmail(email);
        u.setPassword(PasswordUtil.hash(password));
        u.setRole(role == null ? UserRole.OPERATOR.label() : role.label());
        u.setStatus("Active");
        validateUser(u);
        dao.insertOrUpdate(u);
    }

    public void updateUser(User actor, User target, String newPassword) throws SQLException {
        if (actor == null || !AccessControl.can(actor, Permission.MANAGE_USERS)) {
            throw new IllegalArgumentException("Only administrators can update users.");
        }
        if (target == null)
            throw new IllegalArgumentException("User cannot be null");
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
            target.setPassword(PasswordUtil.hash(newPassword));
        } else {
            target.setPassword(null);
        }
        validateUser(target);
        dao.insertOrUpdate(target);
    }

    public void deleteUser(User actor, int userId) throws SQLException {
        if (actor == null || !AccessControl.can(actor, Permission.MANAGE_USERS)) {
            throw new IllegalArgumentException("Only administrators can delete users.");
        }
        dao.deleteById(userId);
    }

}