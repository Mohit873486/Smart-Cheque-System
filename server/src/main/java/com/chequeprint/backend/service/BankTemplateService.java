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

    public List<com.chequeprint.backend.entity.BankTemplate> getAllTemplates() {
        return bankTemplateRepository.findAll();
    }

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
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount createBankAccount(BankAccount bankAccount) {
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

    public List<ChequeTemplate> getTemplatesByBankId(Long bankId) {
        return chequeTemplateRepository.findByBankId(bankId);
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

    public List<TemplateField> getFieldsByTemplateId(Long templateId) {
        return templateFieldRepository.findByTemplateId(templateId);
    }
}
