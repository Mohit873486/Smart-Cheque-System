package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.Invoice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    public boolean insert(Invoice inv) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_no,client_name,amount,issue_date,due_date,status,notes) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, inv.getInvoiceNo());
            ps.setString(2, inv.getClientName());
            ps.setBigDecimal(3, inv.getAmount());
            ps.setDate(4, Date.valueOf(inv.getIssueDate()));
            ps.setDate(5, Date.valueOf(inv.getDueDate()));
            ps.setString(6, inv.getStatus().name());
            ps.setString(7, inv.getNotes());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices ORDER BY created_at DESC";
        try (Statement st = AppConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    

    public boolean update(Invoice inv) throws SQLException {
        String sql = "UPDATE invoices SET client_name=?,amount=?,issue_date=?,due_date=?,status=?,notes=? WHERE id=?";
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, inv.getClientName());
            ps.setBigDecimal(2, inv.getAmount());
            ps.setDate(3, Date.valueOf(inv.getIssueDate()));
            ps.setDate(4, Date.valueOf(inv.getDueDate()));
            ps.setString(5, inv.getStatus().name());
            ps.setString(6, inv.getNotes());
            ps.setInt(7, inv.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement("DELETE FROM invoices WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNo(rs.getString("invoice_no"));
        inv.setClientName(rs.getString("client_name"));
        inv.setAmount(rs.getBigDecimal("amount"));
        Date id = rs.getDate("issue_date"); if (id != null) inv.setIssueDate(id.toLocalDate());
        Date dd = rs.getDate("due_date");   if (dd != null) inv.setDueDate(dd.toLocalDate());
        inv.setStatus(Invoice.Status.valueOf(rs.getString("status")));
        inv.setNotes(rs.getString("notes"));
        return inv;
    }
}