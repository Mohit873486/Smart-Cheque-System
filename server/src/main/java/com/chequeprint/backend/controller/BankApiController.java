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
@RequestMapping("/api/bank")
@CrossOrigin(origins = "*")
public class BankApiController {

    @Autowired
    private BankTemplateService bankTemplateService;

    // GET /api/bank - Get all bank accounts
    @GetMapping
    public ResponseEntity<List<BankAccount>> getAllBanks() {
        List<BankAccount> banks = bankTemplateService.getAllBankAccounts();
        return ResponseEntity.ok(banks);
    }

    // POST /api/bank - Create a new bank account
    @PostMapping
    public ResponseEntity<BankAccount> createBank(@Valid @RequestBody BankAccount bankAccount) {
        BankAccount createdBank = bankTemplateService.createBankAccount(bankAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBank);
    }
}
