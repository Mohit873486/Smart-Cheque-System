package com.chequeprint.backend.controller;

import com.chequeprint.backend.entity.BankAccount;
import com.chequeprint.backend.service.BankTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/bank", "/api/banks"})
@CrossOrigin(origins = "*")
public class BankApiController {

    @Autowired
    private BankTemplateService bankTemplateService;

    // GET /api/bank or GET /api/banks - Get all bank accounts
    @GetMapping
    public ResponseEntity<List<BankAccount>> getAllBanks() {
        List<BankAccount> banks = bankTemplateService.getAllBankAccounts();
        return ResponseEntity.ok(banks);
    }

    // GET /api/banks/{id} - Get one bank account/template owner by id
    @GetMapping("/{id}")
    public ResponseEntity<BankAccount> getBankById(@PathVariable Long id) {
        return bankTemplateService.getBankAccountById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/bank - Create a new bank account
    @PostMapping
    public ResponseEntity<BankAccount> createBank(@Valid @RequestBody BankAccount bankAccount) {
        BankAccount createdBank = bankTemplateService.createBankAccount(bankAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBank);
    }
}
