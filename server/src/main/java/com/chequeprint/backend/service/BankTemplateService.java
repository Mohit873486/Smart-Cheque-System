package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.BankTemplate;
import com.chequeprint.backend.repository.BankTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankTemplateService {

    private final BankTemplateRepository repository;
    private final AuditLogService auditLogService;

    @Autowired
    public BankTemplateService(BankTemplateRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<BankTemplate> getAllTemplates() {
        return repository.findAll();
    }

    public Optional<BankTemplate> getTemplateById(int id) {
        return repository.findById(id);
    }

    public BankTemplate createTemplate(BankTemplate template) {
        if (template.getBankCode() == null || template.getBankCode().isBlank()) {
            throw new IllegalArgumentException("Bank code is required.");
        }
        Optional<BankTemplate> existing = repository.findByBankCode(template.getBankCode());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Bank template with code '" + template.getBankCode() + "' already exists.");
        }
        BankTemplate saved = repository.save(template);
        auditLogService.record(null, "bank_templates", saved.getId(), "INSERT", "Created bank template: " + saved.getBankCode());
        return saved;
    }

    public BankTemplate updateTemplate(int id, BankTemplate updated) {
        return repository.findById(id)
                .map(existing -> {
                    if (!existing.getBankCode().equals(updated.getBankCode())) {
                        Optional<BankTemplate> duplicate = repository.findByBankCode(updated.getBankCode());
                        if (duplicate.isPresent()) {
                            throw new IllegalArgumentException("Bank template with code '" + updated.getBankCode() + "' already exists.");
                        }
                    }
                    existing.setBankName(updated.getBankName());
                    existing.setBankCode(updated.getBankCode());
                    existing.setChequeSize(updated.getChequeSize());
                    existing.setMicr(updated.isMicr());
                    existing.setLogoPath(updated.getLogoPath());
                    BankTemplate saved = repository.save(existing);
                    auditLogService.record(null, "bank_templates", saved.getId(), "UPDATE", "Updated bank template: " + saved.getBankCode());
                    return saved;
                })
                .orElseThrow(() -> new IllegalArgumentException("Bank template not found with ID: " + id));
    }

    public void deleteTemplate(int id) {
        Optional<BankTemplate> existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Bank template not found with ID: " + id);
        }
        repository.deleteById(id);
        auditLogService.record(null, "bank_templates", id, "DELETE", "Deleted bank template: " + existing.get().getBankCode());
    }
}
