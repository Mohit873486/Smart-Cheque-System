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

    // 1. BankAccount Operations
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount createBankAccount(BankAccount bankAccount) {
        return bankAccountRepository.save(bankAccount);
    }

    // 2. ChequeTemplate Operations
    public ChequeTemplate createTemplate(ChequeTemplate template) {
        return chequeTemplateRepository.save(template);
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
