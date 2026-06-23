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

    @Autowired
    public ChequeApiController(ChequeService service) {
        this.service = service;
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
}
