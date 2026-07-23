package com.chequeprint.dao;

import com.chequeprint.model.Bank;
import com.chequeprint.service.BankService;

import java.sql.SQLException;
import java.util.List;

/**
 * BankDAO - Refactored to eliminate direct JDBC/DB connection logic
 * and delegate all data operations to the REST API via BankService.
 */
public class BankDAO {

    private final BankService bankService;

    public BankDAO() {
        this.bankService = new BankService();
    }

    public BankDAO(BankService bankService) {
        this.bankService = bankService;
    }

    public boolean insert(Bank b) throws SQLException {
        try {
            Bank created = bankService.createBank(b);
            if (created != null && created.getId() != null) {
                b.setId(created.getId());
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new SQLException("REST API Insert Error: " + e.getMessage(), e);
        }
    }

    public boolean update(Bank b) throws SQLException {
        try {
            Bank updated = bankService.updateBank(b.getId(), b);
            return updated != null;
        } catch (Exception e) {
            throw new SQLException("REST API Update Error: " + e.getMessage(), e);
        }
    }

    public List<Bank> findAll() throws SQLException {
        try {
            return bankService.getAllBanks();
        } catch (Exception e) {
            throw new SQLException("REST API Fetch All Error: " + e.getMessage(), e);
        }
    }

    public Bank findById(int id) throws SQLException {
        try {
            return bankService.getById(id);
        } catch (Exception e) {
            throw new SQLException("REST API Fetch by ID Error: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) throws SQLException {
        try {
            bankService.deleteBank(id);
            return true;
        } catch (Exception e) {
            throw new SQLException("REST API Delete Error: " + e.getMessage(), e);
        }
    }

    public List<String> findAllNames() throws SQLException {
        try {
            return bankService.findAllNames();
        } catch (Exception e) {
            throw new SQLException("REST API Fetch Names Error: " + e.getMessage(), e);
        }
    }
}