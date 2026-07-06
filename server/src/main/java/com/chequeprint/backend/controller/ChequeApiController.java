// package com.chequeprint.backend.controller;
package com.chequeprint.backend.controller;


import com.chequeprint.backend.entity.Cheque;
import com.chequeprint.backend.service.ChequeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cheques")
public class ChequeApiController {

    private final ChequeService service;
    private final com.chequeprint.backend.service.AuditLogService auditLogService;

    @Autowired
    public ChequeApiController(ChequeService service, com.chequeprint.backend.service.AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<Cheque>> getAllCheques() {
        return ResponseEntity.ok(service.getAllCheques());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cheque> getChequeById(@PathVariable int id) {
        return service.getChequeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cheque> createCheque(@RequestBody Cheque cheque) {
        Cheque created = service.createCheque(cheque);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cheque> updateCheque(@PathVariable int id, @RequestBody Cheque cheque) {
        try {
            Cheque updated = service.updateCheque(id, cheque);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCheque(@PathVariable int id) {
        try {
            service.deleteCheque(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByChequeNo(
            @RequestParam String chequeNo,
            @RequestParam(defaultValue = "0") int excludeId) {
        boolean exists = service.existsByChequeNo(chequeNo, excludeId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Cheque>> searchCheques(@RequestParam String query) {
        return ResponseEntity.ok(service.searchCheques(query));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveCheque(@PathVariable int id) {
        try {
            java.util.Optional<Cheque> opt = service.getChequeById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Cheque cheque = opt.get();
            if (cheque.getStatus() != Cheque.Status.Pending) {
                java.util.Map<String, Object> err = new java.util.HashMap<>();
                err.put("timestamp", java.time.LocalDateTime.now().toString());
                err.put("status", org.springframework.http.HttpStatus.CONFLICT.value());
                err.put("error", "Conflict");
                err.put("message", "Only 'Pending' cheques can be approved. Current status: " + cheque.getStatus());
                err.put("path", "/api/cheques/" + id + "/approve");
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(err);
            }
            cheque.setStatus(Cheque.Status.Approved);
            Cheque updated = service.updateCheque(id, cheque);
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/print")
    public ResponseEntity<?> printCheque(@PathVariable int id) {
        try {
            java.util.Optional<Cheque> opt = service.getChequeById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Cheque cheque = opt.get();
            if (cheque.getStatus() != Cheque.Status.Approved && cheque.getStatus() != Cheque.Status.Printed) {
                java.util.Map<String, Object> err = new java.util.HashMap<>();
                err.put("timestamp", java.time.LocalDateTime.now().toString());
                err.put("status", org.springframework.http.HttpStatus.CONFLICT.value());
                err.put("error", "Conflict");
                err.put("message", "Only 'Approved' cheques can be printed. Current status: " + cheque.getStatus());
                err.put("path", "/api/cheques/" + id + "/print");
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(err);
            }
            cheque.setStatus(Cheque.Status.Printed);
            Cheque updated = service.updateCheque(id, cheque);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", updated.getId());
            response.put("chequeNo", updated.getChequeNo());
            response.put("status", updated.getStatus().name());
            response.put("printedAt", java.time.LocalDateTime.now().toString());
            response.put("pdfDownloadUrl", "http://localhost:8081/api/cheques/" + id + "/pdf");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getChequeHistory(@PathVariable int id) {
        try {
            java.util.Optional<Cheque> opt = service.getChequeById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<com.chequeprint.backend.entity.AuditLog> history = auditLogService.getHistory("cheques", id);
            return ResponseEntity.ok(history);
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
