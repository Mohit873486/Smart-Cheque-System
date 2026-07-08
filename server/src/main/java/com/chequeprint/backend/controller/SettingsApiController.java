package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.Settings;
import com.chequeprint.backend.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsApiController {

    private final SettingsService service;

    @Autowired
    public SettingsApiController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Settings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    public ResponseEntity<Settings> updateSettings(@RequestBody Settings settings) {
        try {
            Settings updated = service.saveSettings(settings);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
