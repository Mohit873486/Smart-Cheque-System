package com.chequeprint.dao;

import com.chequeprint.config.AppConfig;
import com.chequeprint.model.Cheque;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for the cheques table. */
public class ChequeDAO {

    private static final String INSERT =
        "INSERT INTO cheques (cheque_no, payee_name, amount, amount_words, bank_id, issue_date, status) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_ALL =
        "SELECT c.*, b.bank_name FROM cheques c " +
        "LEFT JOIN bank_templates b ON c.bank_id = b.id ORDER BY c.created_at DESC";

    private static final String SELECT_BY_ID =
        "SELECT c.*, b.bank_name FROM cheques c " +
        "LEFT JOIN bank_templates b ON c.bank_id = b.id WHERE c.id = ?";

    private static final String UPDATE =
        "UPDATE cheques SET payee_name=?, amount=?, amount_words=?, bank_id=?, " +
        "issue_date=?, status=? WHERE id=?";

    private static final String UPDATE_STATUS_BY_ID =
        "UPDATE cheques SET status=? WHERE id=?";

    private static final String UPDATE_STATUS_BY_CHEQUE_NO =
        "UPDATE cheques SET status=? WHERE cheque_no=?";

    private static final String DELETE = "DELETE FROM cheques WHERE id=?";

    private static final String COUNT_TOTAL   = "SELECT COUNT(*) FROM cheques";
    private static final String COUNT_PRINTED = "SELECT COUNT(*) FROM cheques WHERE status='Printed'";
    private static final String COUNT_PENDING = "SELECT COUNT(*) FROM cheques WHERE status='Pending'";
    private static final String COUNT_BY_ISSUE_DATE =
        "SELECT COUNT(*) FROM cheques WHERE issue_date = ?";
    private static final String SUM_AMOUNT_RANGE =
        "SELECT COALESCE(SUM(amount),0) FROM cheques WHERE issue_date >= ? AND issue_date < ?";

    // ---- CREATE ----
    public boolean insert(Cheque c) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getChequeNo());
            ps.setString(2, c.getPayeeName());
            ps.setBigDecimal(3, c.getAmount());
            ps.setString(4, c.getAmountWords());
            ps.setInt(5, c.getBankId());
            ps.setDate(6, Date.valueOf(c.getIssueDate()));
            ps.setString(7, c.getStatus().name());
            boolean inserted = ps.executeUpdate() > 0;
            if (inserted) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        c.setId(keys.getInt(1));
                    }
                }
            }
            return inserted;
        }
    }

    // ---- READ ALL ----
    public List<Cheque> findAll() throws SQLException {
        List<Cheque> list = new ArrayList<>();
        try (Statement st = AppConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ---- READ BY ID ----
    public Cheque findById(int id) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ---- UPDATE ----
    public boolean update(Cheque c) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(UPDATE)) {
            ps.setString(1, c.getPayeeName());
            ps.setBigDecimal(2, c.getAmount());
            ps.setString(3, c.getAmountWords());
            ps.setInt(4, c.getBankId());
            ps.setDate(5, Date.valueOf(c.getIssueDate()));
            ps.setString(6, c.getStatus().name());
            ps.setInt(7, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Cheque c, Cheque.Status status) throws SQLException {
        if (c == null || status == null) {
            return false;
        }

        boolean updated = false;
        if (c.getId() > 0) {
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(UPDATE_STATUS_BY_ID)) {
                ps.setString(1, status.name());
                ps.setInt(2, c.getId());
                updated = ps.executeUpdate() > 0;
            }
        }

        if (!updated && c.getChequeNo() != null && !c.getChequeNo().isBlank()) {
            try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(UPDATE_STATUS_BY_CHEQUE_NO)) {
                ps.setString(1, status.name());
                ps.setString(2, c.getChequeNo());
                updated = ps.executeUpdate() > 0;
            }
        }

        if (updated) {
            c.setStatus(status);
        }
        return updated;
    }

    // ---- DELETE ----
    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- COUNTS ----
    public int countTotal() throws SQLException {
        return querySingleInt(COUNT_TOTAL);
    }

    public int countPrinted() throws SQLException {
        return querySingleInt(COUNT_PRINTED);
    }

    public int countPending() throws SQLException {
        return querySingleInt(COUNT_PENDING);
    }

    public int countTodayEntries() throws SQLException {
        return countByIssueDate(LocalDate.now());
    }

    public int countByIssueDate(LocalDate date) throws SQLException {
        if (date == null) {
            return 0;
        }
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(COUNT_BY_ISSUE_DATE)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public double sumThisMonth() throws SQLException {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        try (PreparedStatement ps = AppConfig.getConnection().prepareStatement(SUM_AMOUNT_RANGE)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    // ---- HELPERS ----
    private int querySingleInt(String sql) throws SQLException {
        try (Statement st = AppConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Cheque mapRow(ResultSet rs) throws SQLException {
        Cheque c = new Cheque();
        c.setId(rs.getInt("id"));
        c.setChequeNo(rs.getString("cheque_no"));
        c.setPayeeName(rs.getString("payee_name"));
        c.setAmount(rs.getBigDecimal("amount"));
        c.setAmountWords(rs.getString("amount_words"));
        c.setBankId(rs.getInt("bank_id"));
        c.setBankName(rs.getString("bank_name"));
        Date d = rs.getDate("issue_date");
        if (d != null) c.setIssueDate(d.toLocalDate());
        c.setStatus(Cheque.Status.valueOf(rs.getString("status")));
        return c;
    }
}
