package com.chequeprint.service;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.util.BankTemplateLayoutStore;
import com.chequeprint.util.ChequeSizeCodec;

import java.sql.SQLException;
import java.util.List;

/**
 * Dedicated Template Service handling bank cheque layout templates.
 * Single Responsibility: Layout loading, coordinate decoding/encoding, and template persistence.
 */
public class TemplateService {

    private final BankDAO bankDAO;
    private final BankTemplateLayoutStore layoutStore;

    public TemplateService() {
        this.bankDAO = new BankDAO();
        this.layoutStore = new BankTemplateLayoutStore();
    }

    public TemplateService(BankDAO bankDAO) {
        this.bankDAO = bankDAO;
        this.layoutStore = new BankTemplateLayoutStore();
    }

    public BankTemplateLayout getTemplateForBank(Bank bank) {
        if (bank == null) {
            return getDefaultLayout();
        }

        if (bank.getBankCode() != null && !bank.getBankCode().isBlank()) {
            BankTemplateLayout stored = layoutStore.loadAll().get(bank.getBankCode());
            if (stored != null) {
                stored.ensureAllFields();
                return stored;
            }
        }

        BankTemplateLayout decoded = ChequeSizeCodec.decodeLayout(bank.getChequeSize());
        decoded.ensureAllFields();
        return decoded;
    }

    public BankTemplateLayout getTemplateForBankId(int bankId) throws Exception {
        Bank bank = bankDAO.findById(bankId);
        return getTemplateForBank(bank);
    }

    public boolean saveTemplate(Bank bank, BankTemplateLayout layout) throws Exception {
        if (bank == null || layout == null) {
            throw new IllegalArgumentException("Bank and layout must not be null.");
        }

        layout.ensureAllFields();
        String json = ChequeSizeCodec.encodeLayout(layout);
        bank.setChequeSize(json);

        boolean updated = bankDAO.update(bank);
        if (updated && bank.getBankCode() != null && !bank.getBankCode().isBlank()) {
            var all = layoutStore.loadAll();
            all.put(bank.getBankCode(), layout);
            layoutStore.saveAll(all);
        }
        return updated;
    }

    public BankTemplateLayout getDefaultLayout() {
        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);
        layout.ensureAllFields();
        return layout;
    }
}
