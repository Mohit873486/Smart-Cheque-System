package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.ChequeTemplate;
import com.chequeprint.backend.entity.TemplateField;
import com.chequeprint.backend.service.BankTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/template")
@CrossOrigin(origins = "*")
public class TemplateApiController {

    @Autowired
    private BankTemplateService bankTemplateService;

    // POST /api/template - Create a new cheque template
    @PostMapping
    public ResponseEntity<ChequeTemplate> createTemplate(@Valid @RequestBody ChequeTemplate template) {
        ChequeTemplate createdTemplate = bankTemplateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplate);
    }

    // GET /api/template/{bankId} - Get all templates for a specific bankId
    @GetMapping("/{bankId}")
    public ResponseEntity<List<ChequeTemplate>> getTemplatesByBankId(@PathVariable Long bankId) {
        List<ChequeTemplate> templates = bankTemplateService.getTemplatesByBankId(bankId);
        return ResponseEntity.ok(templates);
    }

    // POST /api/template/fields - Create or update template fields (supports single object or JSON array)
    @PostMapping("/fields")
    public ResponseEntity<?> createTemplateFields(@RequestBody Object fieldsPayload) {
        if (fieldsPayload instanceof List) {
            List<TemplateField> fieldsList = (List<TemplateField>) fieldsPayload;
            List<TemplateField> savedFields = bankTemplateService.saveTemplateFields(fieldsList);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFields);
        } else {
            // Jackson map single object if passed
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                TemplateField singleField = mapper.convertValue(fieldsPayload, TemplateField.class);
                TemplateField savedField = bankTemplateService.saveTemplateField(singleField);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedField);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload format");
            }
        }
    }

    // GET /api/template/fields/{templateId} - Get all template fields for a specific templateId
    @GetMapping("/fields/{templateId}")
    public ResponseEntity<List<TemplateField>> getFieldsByTemplateId(@PathVariable Long templateId) {
        List<TemplateField> fields = bankTemplateService.getFieldsByTemplateId(templateId);
        return ResponseEntity.ok(fields);
    }
}
