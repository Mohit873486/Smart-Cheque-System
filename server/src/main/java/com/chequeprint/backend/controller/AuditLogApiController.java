package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.AuditLog;
import com.chequeprint.backend.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditLogApiController {

    private final AuditLogService service;

    @Autowired
    public AuditLogApiController(AuditLogService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AuditLog> recordLog(@RequestBody AuditLog log) {
        AuditLog created = service.record(log.getUserId(), log.getTableName(), log.getRecordId(), log.getAction(), log.getDetails());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecent(@RequestParam(defaultValue = "50") int limit) {
        List<AuditLog> logs = service.getRecent(limit);
        return ResponseEntity.ok(logs);
    }
}
