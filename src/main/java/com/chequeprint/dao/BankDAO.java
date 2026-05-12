package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.Bank;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BankDAO {

    public boolean insert(Bank b) throws SQLException {
        String sql = "INSERT INTO bank_templates (bank_name,bank_code,cheque_size,micr,logo_path) "
                + "VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getBankName());
            ps.setString(2, b.getBankCode());
            ps.setString(3, b.getChequeSize());
            ps.setBoolean(4, b.isMicr());
            ps.setString(5, b.getLogoPath());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean update(Bank b) throws SQLException {
        String sql = "UPDATE bank_templates SET bank_name=?,bank_code=?,cheque_size=?,micr=?,logo_path=? "
                + "WHERE id=?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getBankName());
            ps.setString(2, b.getBankCode());
            ps.setString(3, b.getChequeSize());
            ps.setBoolean(4, b.isMicr());
            ps.setString(5, b.getLogoPath());
            ps.setInt(6, b.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Bank> findAll() throws SQLException {
        List<Bank> list = new ArrayList<>();
        String sql = "SELECT * FROM bank_templates ORDER BY bank_name";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Bank bt = new Bank();
                bt.setId(rs.getInt("id"));
                bt.setBankName(rs.getString("bank_name"));
                bt.setBankCode(rs.getString("bank_code"));
                bt.setChequeSize(rs.getString("cheque_size"));
                bt.setMicr(rs.getBoolean("micr"));
                bt.setLogoPath(rs.getString("logo_path"));
                list.add(bt);
            }
        }
        return list;
    }

    public Bank findById(int id) throws SQLException {
        String sql = "SELECT * FROM bank_templates WHERE id=?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bank bt = new Bank();
                    bt.setId(rs.getInt("id"));
                    bt.setBankName(rs.getString("bank_name"));
                    bt.setBankCode(rs.getString("bank_code"));
                    bt.setChequeSize(rs.getString("cheque_size"));
                    bt.setMicr(rs.getBoolean("micr"));
                    bt.setLogoPath(rs.getString("logo_path"));
                    return bt;
                }
            }
        }
        return null;
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection()
                .prepareStatement("DELETE FROM bank_templates WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Returns all bank names for combo-boxes. */
    public List<String> findAllNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT bank_name FROM bank_templates ORDER BY bank_name";
        try (Statement st = AppConfig.getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                names.add(rs.getString("bank_name"));
        }
        return names;
    }
}