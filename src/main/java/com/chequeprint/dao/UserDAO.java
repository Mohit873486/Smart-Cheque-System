package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.User;

import java.sql.*;

public class UserDAO {

    public User findFirst() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id LIMIT 1";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                User u = new User();
                u.setUsername(rs.getString("username"));
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setCompany(rs.getString("company"));
                u.setAddress(rs.getString("address"));
                u.setRole(rs.getString("role"));
                return u;
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
                    User u = new User();
                    u.setUsername(rs.getString("username"));
                    u.setId(rs.getInt("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setPhone(rs.getString("phone"));
                    u.setCompany(rs.getString("company"));
                    u.setAddress(rs.getString("address"));
                    u.setPassword(rs.getString("password"));
                    u.setRole(rs.getString("role"));
                    return u;
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
                    return u;
                }
            }
        }
        return null;
    }

    public boolean insertOrUpdate(User u) throws SQLException {
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
}