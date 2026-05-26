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
        validate(c);
        c.setAmountWords(AmountToWords.convert(c.getAmount().doubleValue()));
        if (c.getChequeNo() == null || c.getChequeNo().isBlank()) {
            c.setChequeNo(generateChequeNo());
        }
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
        if (c == null) return false;
        return dao.updateStatus(c, Cheque.Status.Printed);
    }

    // --- Dashboard stats ---
    public int getTotalCheques()   throws SQLException { return dao.countTotal(); }
    public int getPrintedCheques() throws SQLException { return dao.countPrinted(); }
    public int getPendingCheques() throws SQLException { return dao.countPending(); }
    public int getTodayCheques() throws SQLException { return dao.countTodayEntries(); }
    public double getMonthlyAmount() throws SQLException { return dao.sumThisMonth(); }

    // --- Helpers ---
    private void validate(Cheque c) {
        if (c.getPayeeName() == null || c.getPayeeName().isBlank())
            throw new IllegalArgumentException("Payee name is required.");
        if (c.getAmount() == null || c.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero.");
        if (c.getIssueDate() == null)
            c.setIssueDate(LocalDate.now());
    }

    private String generateChequeNo() {
        return "CHQ-" + System.currentTimeMillis();
    }
}
