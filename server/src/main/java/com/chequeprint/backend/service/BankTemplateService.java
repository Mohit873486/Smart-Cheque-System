package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.BankAccount;
import com.chequeprint.backend.entity.ChequeTemplate;
import com.chequeprint.backend.entity.TemplateField;
import com.chequeprint.backend.repository.BankAccountRepository;
import com.chequeprint.backend.repository.ChequeTemplateRepository;
import com.chequeprint.backend.repository.TemplateFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankTemplateService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private ChequeTemplateRepository chequeTemplateRepository;

    @Autowired
    private TemplateFieldRepository templateFieldRepository;

    @Autowired
    private com.chequeprint.backend.repository.BankTemplateRepository bankTemplateRepository;

    @Transactional(readOnly = true)
    public List<com.chequeprint.backend.entity.BankTemplate> getAllTemplates() {
        return bankTemplateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<com.chequeprint.backend.entity.BankTemplate> getTemplateById(int id) {
        return bankTemplateRepository.findById(id);
    }

    public com.chequeprint.backend.entity.BankTemplate createTemplate(com.chequeprint.backend.entity.BankTemplate template) {
        return bankTemplateRepository.save(template);
    }

    public com.chequeprint.backend.entity.BankTemplate updateTemplate(int id, com.chequeprint.backend.entity.BankTemplate template) {
        template.setId(id);
        return bankTemplateRepository.save(template);
    }

    public void deleteTemplate(int id) {
        bankTemplateRepository.deleteById(id);
    }

    // 1. BankAccount Operations
    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<BankAccount> getBankAccountById(Long id) {
        return bankAccountRepository.findById(id);
    }

    public BankAccount createBankAccount(BankAccount bankAccount) {
        if (bankAccount.getAccountNumber() != null && bankAccountRepository.findByAccountNumber(bankAccount.getAccountNumber().trim()).isPresent()) {
            throw new IllegalArgumentException("Bank account with account number '" + bankAccount.getAccountNumber() + "' already exists.");
        }
        if (bankAccount.getTemplateId() == null) {
            bankAccount.setTemplateId(1L);
        }
        return bankAccountRepository.save(bankAccount);
    }

    // 2. ChequeTemplate Operations
    public ChequeTemplate createTemplate(ChequeTemplate template) {
        if (template.getId() != null) {
            java.util.Optional<ChequeTemplate> existingById = chequeTemplateRepository.findById(template.getId());
            if (existingById.isPresent()) {
                ChequeTemplate existing = existingById.get();
                updateTemplateProperties(existing, template);
                return chequeTemplateRepository.save(existing);
            }
        }
        if (template.getBankId() != null) {
            List<ChequeTemplate> existingByBank = chequeTemplateRepository.findByBankId(template.getBankId());
            if (!existingByBank.isEmpty()) {
                ChequeTemplate existing = existingByBank.get(0);
                updateTemplateProperties(existing, template);
                return chequeTemplateRepository.save(existing);
            }
        }
        return chequeTemplateRepository.save(template);
    }

    private void updateTemplateProperties(ChequeTemplate existing, ChequeTemplate template) {
        existing.setTemplateName(template.getTemplateName());
        if (template.getWidth() != null) existing.setWidth(template.getWidth());
        if (template.getHeight() != null) existing.setHeight(template.getHeight());
        if (template.getConfigJson() != null) existing.setConfigJson(template.getConfigJson());
    }

    @Transactional(readOnly = true)
    public List<ChequeTemplate> getTemplatesByBankId(Long bankId) {
        return chequeTemplateRepository.findByBankId(bankId);
    }

    @Transactional(readOnly = true)
    public List<ChequeTemplate> getTemplatesForAccount(Long accountId) {
        if (accountId == null) return List.of();
        return chequeTemplateRepository.findByAccountId(accountId);
    }

    public ChequeTemplate setTemplateAsDefaultForAccount(Long accountId, Long templateId) {
        if (accountId == null || templateId == null) return null;
        
        // Reset default flag on all templates for this account
        List<ChequeTemplate> templates = chequeTemplateRepository.findByAccountId(accountId);
        for (ChequeTemplate t : templates) {
            t.setIsDefault(t.getId().equals(templateId));
            chequeTemplateRepository.save(t);
        }

        // Update BankAccount's primary templateId
        bankAccountRepository.findById(accountId).ifPresent(acc -> {
            acc.setDefaultTemplateId(templateId);
            bankAccountRepository.save(acc);
        });

        return chequeTemplateRepository.findById(templateId).orElse(null);
    }

    // 3. TemplateField Operations
    public List<TemplateField> saveTemplateFields(List<TemplateField> fields) {
        if (fields != null && !fields.isEmpty()) {
            Long templateId = fields.get(0).getTemplateId();
            if (templateId != null) {
                templateFieldRepository.deleteByTemplateId(templateId);
            }
            return templateFieldRepository.saveAll(fields);
        }
        return List.of();
    }

    public TemplateField saveTemplateField(TemplateField field) {
        return templateFieldRepository.save(field);
    }

    @Transactional(readOnly = true)
    public List<TemplateField> getFieldsByTemplateId(Long templateId) {
        return templateFieldRepository.findByTemplateId(templateId);
    }
}
