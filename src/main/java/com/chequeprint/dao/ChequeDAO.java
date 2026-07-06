package com.chequeprint.dao;

import com.chequeprint.model.Cheque;
import com.chequeprint.util.ChequeApiClient;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Data Access Object for the cheques table (rewritten to use Spring Boot REST API). */
public class ChequeDAO {

    private final ChequeApiClient client = new ChequeApiClient();

    // ---- CREATE ----
    public boolean insert(Cheque c) throws SQLException {
        try {
            Cheque created = client.createCheque(c);
            c.setId(created.getId());
            return true;
        } catch (Exception e) {
            throw new SQLException("Failed to insert cheque via REST API", e);
        }
    }

    // ---- READ ALL ----
    public List<Cheque> findAll() throws SQLException {
        try {
            return client.getAllCheques();
        } catch (Exception e) {
            throw new SQLException("Failed to fetch cheques from REST API", e);
        }
    }

    // ---- READ BY ID ----
    public Cheque findById(int id) throws SQLException {
        try {
            return client.getChequeById(id);
        } catch (Exception e) {
            throw new SQLException("Failed to find cheque from REST API", e);
        }
    }

    // ---- UPDATE ----
    public boolean update(Cheque c) throws SQLException {
        try {
            return client.updateCheque(c);
        } catch (Exception e) {
            throw new SQLException("Failed to update cheque via REST API", e);
        }
    }

    public boolean updateStatus(Cheque c, Cheque.Status status) throws SQLException {
        if (c == null || status == null) {
            return false;
        }
        c.setStatus(status);
        return update(c);
    }

    // ---- DELETE ----
    public boolean delete(int id) throws SQLException {
        try {
            return client.deleteCheque(id);
        } catch (Exception e) {
            throw new SQLException("Failed to delete cheque via REST API", e);
        }
    }

    // Convenience API requested by product requirements
    public boolean saveCheque(Cheque cheque) throws SQLException {
        return insert(cheque);
    }

    public boolean updateChequeStatus(Cheque cheque, Cheque.Status status) throws SQLException {
        return updateStatus(cheque, status);
    }

    public List<Cheque> getAllCheques() throws SQLException {
        return findAll();
    }

    // ---- COUNTS (computed in memory) ----
    public int countTotal() throws SQLException {
        return findAll().size();
    }

    public int countPrinted() throws SQLException {
        return (int) findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Printed)
                .count();
    }

    public int countPending() throws SQLException {
        return (int) findAll().stream()
                .filter(c -> c.getStatus() == Cheque.Status.Pending)
                .count();
    }

    public int countTodayEntries() throws SQLException {
        return countByIssueDate(LocalDate.now());
    }

    public int countByIssueDate(LocalDate date) throws SQLException {
        if (date == null) {
            return 0;
        }
        return (int) findAll().stream()
                .filter(c -> c.getIssueDate() != null && c.getIssueDate().equals(date))
                .count();
    }

    public double sumThisMonth() throws SQLException {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return findAll().stream()
                .filter(c -> c.getIssueDate() != null && !c.getIssueDate().isBefore(start) && c.getIssueDate().isBefore(end))
                .map(c -> c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    public boolean existsByChequeNo(String chequeNo, int excludeId) throws SQLException {
        if (chequeNo == null || chequeNo.isBlank()) {
            return false;
        }

        try {
            return client.existsByChequeNo(chequeNo, excludeId);
        } catch (Exception e) {
            System.err.println("REST server unavailable while checking cheque number; continuing without duplicate validation: " + e.getMessage());
            return false;
        }
    }

    public List<Cheque> search(String query) throws SQLException {
        try {
            return client.searchCheques(query);
        } catch (Exception e) {
            throw new SQLException("Failed to search cheques via REST API", e);
        }
    }
}
