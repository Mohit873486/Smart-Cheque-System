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

    public Optional<ChequeTemplate> getTemplateByBankId(Long bankId) {
        List<ChequeTemplate> list = chequeTemplateRepository.findByBankId(bankId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public ChequeTemplate saveOrUpdateTemplate(ChequeTemplate template) {
        List<ChequeTemplate> existingList = chequeTemplateRepository.findByBankId(template.getBankId());
        
        if (!existingList.isEmpty()) {
            ChequeTemplate toUpdate = existingList.get(0);
            toUpdate.setTemplateName(template.getTemplateName());
            if (template.getWidth() != null) toUpdate.setWidth(template.getWidth());
            if (template.getHeight() != null) toUpdate.setHeight(template.getHeight());
            if (template.getConfigJson() != null) toUpdate.setConfigJson(template.getConfigJson());
            return chequeTemplateRepository.save(toUpdate);
        } else {
            return chequeTemplateRepository.save(template);
        }
    }
}
