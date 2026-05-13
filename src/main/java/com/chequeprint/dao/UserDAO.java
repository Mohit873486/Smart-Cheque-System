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
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setCompany(rs.getString("company"));
                u.setAddress(rs.getString("address"));
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
                    u.setId(rs.getInt("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setPhone(rs.getString("phone"));
                    u.setCompany(rs.getString("company"));
                    u.setAddress(rs.getString("address"));
                    u.setPassword(rs.getString("password"));
                    return u;
                }
            }
        }
        return null;
    }

    public boolean insertOrUpdate(User u) throws SQLException {
        if (u.getId() == 0) {
            String sql = "INSERT INTO users (name,email,phone,company,address,password) VALUES(?,?,?,?,?,?)";
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
                ps.setString(1, u.getName());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getPhone());
                ps.setString(4, u.getCompany());
                ps.setString(5, u.getAddress());
                ps.setString(6, u.getPassword());
                return ps.executeUpdate() > 0;
            }
        } else {
            String sql = "UPDATE users SET name=?,email=?,phone=?,company=?,address=?,password=? WHERE id=?";
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
                ps.setString(1, u.getName());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getPhone());
                ps.setString(4, u.getCompany());
                ps.setString(5, u.getAddress());
                ps.setString(6, u.getPassword());
                ps.setInt(7, u.getId());
                return ps.executeUpdate() > 0;
            }
        }
    }
}