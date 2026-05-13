package com.chequeprint.service;

import com.chequeprint.dao.InvoiceDAO;
import com.chequeprint.model.Invoice;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InvoiceService {

    private final InvoiceDAO dao = new InvoiceDAO();
    

    public List<Invoice> getAll() throws SQLException { return dao.findAll(); }

    public boolean save(Invoice inv) throws SQLException {
        validate(inv);
        if (inv.getInvoiceNo() == null || inv.getInvoiceNo().isBlank())
            inv.setInvoiceNo("INV-" + System.currentTimeMillis());
        return dao.insert(inv);
    }

    public boolean update(Invoice inv) throws SQLException {
        validate(inv);
        return dao.update(inv);
    }

    public boolean delete(int id) throws SQLException { return dao.delete(id); }

    public boolean markPaid(int id) throws SQLException {
        List<Invoice> all = dao.findAll();
        Invoice inv = all.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
        if (inv == null) return false;
        inv.setStatus(Invoice.Status.Paid);
        return dao.update(inv);
    }

    private void validate(Invoice inv) {
        if (inv.getClientName() == null || inv.getClientName().isBlank())
            throw new IllegalArgumentException("Client name is required.");
        if (inv.getAmount() == null || inv.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invoice amount must be greater than zero.");
        if (inv.getIssueDate() == null) inv.setIssueDate(LocalDate.now());
        if (inv.getDueDate() == null)   inv.setDueDate(LocalDate.now().plusDays(30));
    }
}