package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.ChequeTemplate;
import com.chequeprint.backend.repository.ChequeTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChequeTemplateService {

    @Autowired
    private ChequeTemplateRepository chequeTemplateRepository;

    public ChequeTemplate getTemplateByBankId(Long bankId) {
        if (bankId == null || bankId <= 0) {
            throw new com.chequeprint.backend.exception.ResourceNotFoundException("Invalid or missing Bank ID: " + bankId);
        }

        List<ChequeTemplate> list = chequeTemplateRepository.findByBankId(bankId);
        if (!list.isEmpty()) {
            return list.get(0);
        }

        // Return a valid default template with standard cheque dimensions (200mm x 93mm)
        ChequeTemplate defaultTemplate = new ChequeTemplate();
        defaultTemplate.setBankId(bankId);
        defaultTemplate.setTemplateName("Default Bank Layout");
        defaultTemplate.setWidth(200.0);
        defaultTemplate.setHeight(93.0);

        try {
            return chequeTemplateRepository.save(defaultTemplate);
        } catch (Exception e) {
            // Fallback if save fails (e.g. read-only DB)
            return defaultTemplate;
        }
    }

    public ChequeTemplate saveOrUpdateTemplate(ChequeTemplate template) {
        if (template.getId() != null) {
            Optional<ChequeTemplate> existingById = chequeTemplateRepository.findById(template.getId());
            if (existingById.isPresent()) {
                ChequeTemplate toUpdate = existingById.get();
                toUpdate.setTemplateName(template.getTemplateName());
                if (template.getWidth() != null) toUpdate.setWidth(template.getWidth());
                if (template.getHeight() != null) toUpdate.setHeight(template.getHeight());
                return chequeTemplateRepository.save(toUpdate);
            }
        }
        
        List<ChequeTemplate> existingList = chequeTemplateRepository.findByBankId(template.getBankId());
        if (!existingList.isEmpty()) {
            ChequeTemplate toUpdate = existingList.get(0);
            toUpdate.setTemplateName(template.getTemplateName());
            if (template.getWidth() != null) toUpdate.setWidth(template.getWidth());
            if (template.getHeight() != null) toUpdate.setHeight(template.getHeight());
            return chequeTemplateRepository.save(toUpdate);
        } else {
            return chequeTemplateRepository.save(template);
        }
    }
}
