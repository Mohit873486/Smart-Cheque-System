package com.chequeprint.backend.controller;

import com.chequeprint.backend.service.BankTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/template-fields")
@CrossOrigin(origins = "*")
public class TemplateFieldsApiController {

    @Autowired
    private BankTemplateService bankTemplateService;

    // POST /api/template-fields - Receive JSON array of [{ "key": "DATE", "x": 100, "y": 50 }]
    @PostMapping
    public ResponseEntity<?> updateTemplateFields(@RequestBody List<Map<String, Object>> fieldsPayload) {
        System.out.println("Received POST /api/template-fields payload: " + fieldsPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "count", fieldsPayload.size()));
    }
}
