package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.Account;
import com.chequeprint.backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountService {

    private final AccountRepository repository;
    private final AuditLogService auditLogService;

    @Autowired
    public AccountService(AccountRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<Account> getAllAccounts() {
        return repository.findAll();
    }

    public Optional<Account> getAccountById(int id) {
        return repository.findById(id);
    }

    public Account createAccount(Account account) {
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        Optional<Account> existing = repository.findByAccountNumber(account.getAccountNumber());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Account number '" + account.getAccountNumber() + "' already exists.");
        }
        Account saved = repository.save(account);
        auditLogService.record(null, "accounts", saved.getId(), "INSERT", "Created account: " + saved.getAccountNumber());
        return saved;
    }

    public Account updateAccount(int id, Account updatedAccount) {
        return repository.findById(id)
                .map(existing -> {
                    // Check if updated account number is duplicate
                    if (!existing.getAccountNumber().equals(updatedAccount.getAccountNumber())) {
                        Optional<Account> duplicate = repository.findByAccountNumber(updatedAccount.getAccountNumber());
                        if (duplicate.isPresent()) {
                            throw new IllegalArgumentException("Account number '" + updatedAccount.getAccountNumber() + "' already exists.");
                        }
                    }
                    existing.setAccountNumber(updatedAccount.getAccountNumber());
                    existing.setAccountHolderName(updatedAccount.getAccountHolderName());
                    existing.setBankName(updatedAccount.getBankName());
                    existing.setBranchName(updatedAccount.getBranchName());
                    existing.setIfscCode(updatedAccount.getIfscCode());
                    existing.setBalance(updatedAccount.getBalance());
                    Account saved = repository.save(existing);
                    auditLogService.record(null, "accounts", saved.getId(), "UPDATE", "Updated account: " + saved.getAccountNumber());
                    return saved;
                })
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
    }

    public void deleteAccount(int id) {
        Optional<Account> existing = repository.findById(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Account not found with ID: " + id);
        }
        repository.deleteById(id);
        auditLogService.record(null, "accounts", id, "DELETE", "Deleted account: " + existing.get().getAccountNumber());
    }
}
