package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.ChequeTemplate;
import com.chequeprint.backend.repository.ChequeTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChequeTemplateService {

    @Autowired
    private ChequeTemplateRepository chequeTemplateRepository;

    public Optional<ChequeTemplate> getTemplateByBankId(Long bankId) {
        return chequeTemplateRepository.findByBankId(bankId);
    }

    public ChequeTemplate saveOrUpdateTemplate(ChequeTemplate template) {
        Optional<ChequeTemplate> existing = chequeTemplateRepository.findByBankId(template.getBankId());
        
        if (existing.isPresent()) {
            ChequeTemplate toUpdate = existing.get();
            toUpdate.setNameX(template.getNameX());
            toUpdate.setNameY(template.getNameY());
            toUpdate.setDateX(template.getDateX());
            toUpdate.setDateY(template.getDateY());
            toUpdate.setAmountX(template.getAmountX());
            toUpdate.setAmountY(template.getAmountY());
            toUpdate.setSignatureX(template.getSignatureX());
            toUpdate.setSignatureY(template.getSignatureY());
            return chequeTemplateRepository.save(toUpdate);
        } else {
            return chequeTemplateRepository.save(template);
        }
    }
}
