package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.BankTemplateLayoutStore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service class for Bank Template operations, serving as the business logic layer
 * between Controllers and BankDAO / BankTemplateLayoutStore.
 */
public class BankService {

    private final BankDAO dao = new BankDAO();
    private final BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();

    public List<Bank> getAll() throws SQLException {
        return dao.findAll();
    }

    public Bank getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public void save(Bank bank, BankTemplateLayout layout, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        validate(bank);
        String code = bank.getBankCode().trim().toUpperCase();

        if (bank.getId() == 0) {
            dao.insert(bank);
        } else {
            // Find existing bank to handle code change
            Bank existing = dao.findById(bank.getId());
            String oldCode = existing != null ? existing.getBankCode().trim().toUpperCase() : "";
            dao.update(bank);
            if (!oldCode.isEmpty() && !oldCode.equals(code)) {
                BankTemplateLayout moved = layoutByBankCode.remove(oldCode);
                if (moved != null) {
                    layoutByBankCode.put(code, moved);
                }
            }
        }

        layoutByBankCode.put(code, layout.copy());
        layoutStore.saveAll(layoutByBankCode);
    }

    public void delete(Bank bank, Map<String, BankTemplateLayout> layoutByBankCode) throws Exception {
        dao.delete(bank.getId());
        String code = bank.getBankCode() != null ? bank.getBankCode().trim().toUpperCase() : "";
        if (!code.isEmpty()) {
            layoutByBankCode.remove(code);
        }
        layoutStore.saveAll(layoutByBankCode);
    }

    public Map<String, BankTemplateLayout> loadAllLayouts() {
        return layoutStore.loadAll();
    }

    public void saveLayouts(Map<String, BankTemplateLayout> layouts) throws IOException {
        layoutStore.saveAll(layouts);
    }

    private void validate(Bank bank) {
        if (bank.getBankName() == null || bank.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        if (bank.getBankCode() == null || bank.getBankCode().isBlank()) {
            throw new IllegalArgumentException("Bank code is required.");
        }
    }
}
