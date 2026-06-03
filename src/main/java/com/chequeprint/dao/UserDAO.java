package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserDAO {

    public User findFirst() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id LIMIT 1";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? mapUser(rs) : null;
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public User findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public boolean insertOrUpdate(User u) throws SQLException {
        if (u.getId() == 0) {
            String sql = """
                    INSERT INTO users
                    (username, name, email, phone, company, address, password, role, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
                bindProfile(ps, u);
                ps.setString(7, u.getPassword());
                ps.setString(8, u.getRole());
                ps.setString(9, u.getStatus());
                return ps.executeUpdate() > 0;
            }
        }

        if (u.getPassword() != null && !u.getPassword().isEmpty()) {
            String sql = """
                    UPDATE users
                    SET username = ?, name = ?, email = ?, phone = ?, company = ?, address = ?,
                        password = ?, role = ?, status = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
                bindProfile(ps, u);
                ps.setString(7, u.getPassword());
                ps.setString(8, u.getRole());
                ps.setString(9, u.getStatus());
                ps.setInt(10, u.getId());
                return ps.executeUpdate() > 0;
            }
        }

        String sql = """
                UPDATE users
                SET username = ?, name = ?, email = ?, phone = ?, company = ?, address = ?, role = ?, status = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            bindProfile(ps, u);
            ps.setString(7, u.getRole());
            ps.setString(8, u.getStatus());
            ps.setInt(9, u.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public void recordFailedLogin(int userId, int maxAttempts) throws SQLException {
        String sql = """
                UPDATE users
                SET failed_login_attempts = failed_login_attempts + 1,
                    account_locked = CASE WHEN failed_login_attempts + 1 >= ? THEN TRUE ELSE account_locked END,
                    locked_at = CASE WHEN failed_login_attempts + 1 >= ? THEN CURRENT_TIMESTAMP ELSE locked_at END
                WHERE id = ?
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, maxAttempts);
            ps.setInt(2, maxAttempts);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void resetLoginFailures(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = 0, account_locked = FALSE, locked_at = NULL WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = """
                UPDATE users
                SET password = ?, failed_login_attempts = 0, account_locked = FALSE, locked_at = NULL
                WHERE id = ?
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void createPasswordResetOtp(int userId, String otpHash, LocalDateTime expiresAt) throws SQLException {
        String sql = """
                INSERT INTO password_reset_otps (user_id, otp_hash, expires_at)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, otpHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public String findActiveOtpHash(int userId) throws SQLException {
        String sql = """
                SELECT otp_hash
                FROM password_reset_otps
                WHERE user_id = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                ORDER BY id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("otp_hash") : null;
            }
        }
    }

    public void markOtpsUsed(int userId) throws SQLException {
        String sql = "UPDATE password_reset_otps SET used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND used_at IS NULL";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void bindProfile(PreparedStatement ps, User u) throws SQLException {
        ps.setString(1, u.getUsername());
        ps.setString(2, u.getName());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPhone());
        ps.setString(5, u.getCompany());
        ps.setString(6, u.getAddress());
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setCompany(rs.getString("company"));
        u.setAddress(rs.getString("address"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        u.setAccountLocked(rs.getBoolean("account_locked"));
        Timestamp lockedAt = rs.getTimestamp("locked_at");
        u.setLockedAt(lockedAt == null ? null : lockedAt.toLocalDateTime());
        return u;
    }
}
