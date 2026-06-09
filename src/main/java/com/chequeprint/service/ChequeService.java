package com.chequeprint.service;

import com.chequeprint.dao.ChequeDAO;
import com.chequeprint.model.Cheque;
import com.chequeprint.util.AmountToWords;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * ChequeService — business logic layer between controllers and DAO.
 * Validates input, converts amount to words, generates cheque numbers.
 */
public class ChequeService {

    private final ChequeDAO dao = new ChequeDAO();

    public List<Cheque> getAll() throws SQLException {
        return dao.findAll();
    }

    public Cheque getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public boolean save(Cheque c) throws SQLException {
        if (c.getChequeNo() == null || c.getChequeNo().isBlank()) {
            c.setChequeNo(generateChequeNo());
            while (dao.existsByChequeNo(c.getChequeNo(), c.getId())) {
                c.setChequeNo(generateChequeNo());
            }
        }
        validate(c);
        c.setAmountWords(AmountToWords.convert(c.getAmount().doubleValue()));
        return dao.insert(c);
    }

    public boolean update(Cheque c) throws SQLException {
        validate(c);
        c.setAmountWords(AmountToWords.convert(c.getAmount().doubleValue()));
        return dao.update(c);
    }

    public boolean delete(int id) throws SQLException {
        return dao.delete(id);
    }

    public boolean markPrinted(int id) throws SQLException {
        Cheque c = dao.findById(id);
        if (c == null)
            return false;
        return dao.updateStatus(c, Cheque.Status.Printed);
    }

    public boolean setStatus(Cheque cheque, Cheque.Status status) throws SQLException {
        if (cheque == null || status == null) {
            return false;
        }
        return dao.updateStatus(cheque, status);
    }

    // --- Dashboard stats ---
    public int getTotalCheques() throws SQLException {
        return dao.countTotal();
    }

    public int getPrintedCheques() throws SQLException {
        return dao.countPrinted();
    }

    public int getPendingCheques() throws SQLException {
        return dao.countPending();
    }

    public int getTodayCheques() throws SQLException {
        return dao.countTodayEntries();
    }

    public double getMonthlyAmount() throws SQLException {
        return dao.sumThisMonth();
    }

    public int getCountByDate(java.time.LocalDate date) throws SQLException {
        return dao.countByIssueDate(date);
    }

    // --- Helpers ---
    private void validate(Cheque c) throws SQLException {
        if (c.getPayeeName() == null || c.getPayeeName().isBlank())
            throw new IllegalArgumentException("Payee name is required.");
        if (c.getPayeeName().length() > 150)
            throw new IllegalArgumentException("Payee name cannot exceed 150 characters.");
        if (!c.getPayeeName().matches("^[a-zA-Z0-9 .'-]+$"))
            throw new IllegalArgumentException("Payee name contains invalid characters.");

        if (c.getAmount() == null || c.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero.");
        if (c.getAmount().compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("Amount exceeds maximum allowed limit.");

        if (c.getIssueDate() == null) {
            c.setIssueDate(LocalDate.now());
        } else {
            LocalDate today = LocalDate.now();
            if (c.getIssueDate().isBefore(today.minusDays(90))) {
                throw new IllegalArgumentException("Cheque date cannot be older than 90 days (stale cheque).");
            }
            if (c.getIssueDate().isAfter(today.plusDays(180))) {
                throw new IllegalArgumentException("Cheque date cannot be more than 180 days in the future.");
            }
        }

        if (c.getChequeNo() != null && !c.getChequeNo().isBlank()) {
            if (dao.existsByChequeNo(c.getChequeNo(), c.getId())) {
                throw new IllegalArgumentException("Cheque number '" + c.getChequeNo() + "' already exists.");
            }
        }
    }

    private String generateChequeNo() {
        return "CHQ-" + System.currentTimeMillis();
    }
}
