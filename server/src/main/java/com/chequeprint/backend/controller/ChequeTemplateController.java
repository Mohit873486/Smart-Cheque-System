package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.ChequeTemplate;
import com.chequeprint.backend.service.ChequeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template")
@CrossOrigin(origins = "*")
public class ChequeTemplateController {

    @Autowired
    private ChequeTemplateService chequeTemplateService;

    @GetMapping("/bank/{bankId}")
    public ResponseEntity<ChequeTemplate> getTemplateByBankId(@PathVariable Long bankId) {
        return chequeTemplateService.getTemplateByBankId(bankId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ChequeTemplate> saveTemplate(@RequestBody ChequeTemplate template) {
        ChequeTemplate savedTemplate = chequeTemplateService.saveOrUpdateTemplate(template);
        return ResponseEntity.ok(savedTemplate);
    }
}
