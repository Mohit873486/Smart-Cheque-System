package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.BankTemplate;
import com.chequeprint.backend.service.BankTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
public class BankTemplateApiController {

    private final BankTemplateService service;

    @Autowired
    public BankTemplateApiController(BankTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BankTemplate>> getAllTemplates() {
        return ResponseEntity.ok(service.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankTemplate> getTemplateById(@PathVariable int id) {
        return service.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody BankTemplate template) {
        try {
            BankTemplate created = service.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable int id, @RequestBody BankTemplate template) {
        try {
            BankTemplate updated = service.updateTemplate(id, template);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable int id) {
        try {
            service.deleteTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
