package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.Settings;
import com.chequeprint.backend.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final SettingsRepository repository;

    @Autowired
    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Settings getSettings() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            Settings defaultSettings = new Settings();
            return repository.save(defaultSettings);
        });
    }

    @Transactional
    public Settings saveSettings(Settings s) {
        Settings existing = repository.findAll().stream().findFirst().orElseGet(() -> {
            Settings defaultSettings = new Settings();
            return defaultSettings;
        });

        existing.setAppName(s.getAppName());
        existing.setCurrency(s.getCurrency());
        existing.setDateFormat(s.getDateFormat());
        existing.setLanguage(s.getLanguage());
        existing.setChequePrefix(s.getChequePrefix());
        existing.setDefaultBank(s.getDefaultBank());
        existing.setAutoPrint(s.isAutoPrint());
        existing.setAmountConfirm(s.isAmountConfirm());
        existing.setInvoicePrefix(s.getInvoicePrefix());
        existing.setPaymentTerms(s.getPaymentTerms());
        existing.setAutoGST(s.isAutoGST());
        existing.setTheme(s.getTheme());

        return repository.save(existing);
    }
}
