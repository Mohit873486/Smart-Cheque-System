package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.User;

import java.sql.*;

public class UserDAO {

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
        u.setStatus(readStringIfPresent(rs, "status"));
        u.setLoginAttempts(readIntIfPresent(rs, "login_attempts", "failed_login_attempts"));
        u.setAccountLocked(readBooleanIfPresent(rs, "account_locked"));
        return u;
    }

    private String readStringIfPresent(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
        }
    }

    private int readIntIfPresent(ResultSet rs, String... columns) throws SQLException {
        for (String column : columns) {
            try {
                return rs.getInt(column);
            } catch (SQLException ignored) {
                // Try the next compatible schema column.
            }
        }
        return 0;
    }

    private boolean readBooleanIfPresent(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getBoolean(column);
        } catch (SQLException ex) {
            return false;
        }
    }

    public User findFirst() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id LIMIT 1";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    public User findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    public User findByUsernameOrEmailAndRole(String usernameOrEmail, String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND role = ? LIMIT 1";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    public void createPasswordResetOtp(int userId, String otpHash, java.time.LocalDateTime expiresAt)
            throws SQLException {
        String sql = "INSERT INTO password_reset_otps (user_id, otp_hash, expires_at) VALUES (?, ?, ?)";
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
                WHERE user_id = ?
                  AND used_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("otp_hash");
                }
            }
        }
        return null;
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        if (passwordHash != null && !com.chequeprint.util.PasswordUtil.isBcryptHash(passwordHash)) {
            passwordHash = com.chequeprint.util.PasswordUtil.hash(passwordHash);
        }
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public int incrementFailedLoginAttempts(int userId) throws SQLException {
        String attemptsColumn = loginAttemptColumn();
        String sql = "UPDATE users "
                + "SET " + attemptsColumn + " = " + attemptsColumn + " + 1, "
                + "account_locked = " + attemptsColumn + " + 1 >= 3, "
                + "locked_at = CASE WHEN " + attemptsColumn + " + 1 >= 3 THEN CURRENT_TIMESTAMP ELSE locked_at END "
                + "WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }

        String selectSql = "SELECT " + attemptsColumn + " FROM users WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(attemptsColumn);
                }
            }
        }
        return 0;
    }

    public void resetLoginAttempts(int userId) throws SQLException {
        String attemptsColumn = loginAttemptColumn();
        String sql = "UPDATE users SET " + attemptsColumn
                + " = 0, account_locked = FALSE, locked_at = NULL WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private String loginAttemptColumn() throws SQLException {
        DatabaseMetaData metaData = AppConfig.getConnection().getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, "users", "login_attempts")) {
            if (rs.next()) {
                return "login_attempts";
            }
        }
        return "failed_login_attempts";
    }

    public void lockUser(int userId) throws SQLException {
        String sql = "UPDATE users SET account_locked = TRUE, locked_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void markOtpsUsed(int userId) throws SQLException {
        String sql = "UPDATE password_reset_otps SET used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND used_at IS NULL";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean insertOrUpdate(User u) throws SQLException {
        if (u.getPassword() != null && !u.getPassword().isEmpty() && !com.chequeprint.util.PasswordUtil.isBcryptHash(u.getPassword())) {
            u.setPassword(com.chequeprint.util.PasswordUtil.hash(u.getPassword()));
        }
        if (u.getId() == 0) {

            String sql = "INSERT INTO users (username,name,email,phone,company,address,password,role) VALUES(?,?,?,?,?,?,?,?)";

            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {

                ps.setString(1, u.getUsername());
                ps.setString(2, u.getName());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getPhone());
                ps.setString(5, u.getCompany());
                ps.setString(6, u.getAddress());
                ps.setString(7, u.getPassword());
                ps.setString(8, u.getRole());

                return ps.executeUpdate() > 0;
            }

        } else if (u.getPassword() != null && !u.getPassword().isEmpty()) {

            String sql = "UPDATE users SET username=?,name=?,email=?,phone=?,company=?,address=?,password=?,role=? WHERE id=?";

            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {

                ps.setString(1, u.getUsername());
                ps.setString(2, u.getName());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getPhone());
                ps.setString(5, u.getCompany());
                ps.setString(6, u.getAddress());
                ps.setString(7, u.getPassword());
                ps.setString(8, u.getRole());
                ps.setInt(9, u.getId());

                return ps.executeUpdate() > 0;
            }

        } else {

            String sql = "UPDATE users SET username=?,name=?,email=?,phone=?,company=?,address=?,role=? WHERE id=?";

            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {

                ps.setString(1, u.getUsername());
                ps.setString(2, u.getName());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getPhone());
                ps.setString(5, u.getCompany());
                ps.setString(6, u.getAddress());
                ps.setString(7, u.getRole());
                ps.setInt(8, u.getId());

                return ps.executeUpdate() > 0;
            }
        }

    }

    public java.util.List<User> findAll() throws SQLException {
        java.util.List<User> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id DESC";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        }
        return list;
    }

    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
