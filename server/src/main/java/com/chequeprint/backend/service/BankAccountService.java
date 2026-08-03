package com.chequeprint.backend.service;

import com.chequeprint.backend.dto.BankAccountResponse;
import com.chequeprint.backend.entity.BankAccount;
import com.chequeprint.backend.exception.ResourceNotFoundException;
import com.chequeprint.backend.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Autowired
    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getAllBankAccounts() {
        List<BankAccount> accounts = bankAccountRepository.findAllByOrderByIdAsc();
        if (accounts == null || accounts.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return accounts.stream()
                .filter(java.util.Objects::nonNull)
                .map(BankAccountResponse::from)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankAccount getBankAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Account not found with id: " + id));
    }

    @Transactional
    public BankAccount createBankAccount(BankAccount bankAccount) {
        normalize(bankAccount);
        if (bankAccount.getAccountNumber() != null && bankAccountRepository.findByAccountNumber(bankAccount.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("Bank account with account number '" + bankAccount.getAccountNumber() + "' already exists.");
        }
        if (bankAccount.getTemplateId() == null) {
            bankAccount.setTemplateId(1L);
        }
        return bankAccountRepository.save(bankAccount);
    }

    @Transactional
    public BankAccount updateBankAccount(Long id, BankAccount updatedBankAccount) {
        normalize(updatedBankAccount);
        BankAccount existingAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Account not found with id: " + id));

        if (bankAccountRepository.existsByAccountNumberAndIdNot(updatedBankAccount.getAccountNumber(), id)) {
            throw new IllegalArgumentException("Bank Account with account number " + updatedBankAccount.getAccountNumber() + " already exists.");
        }

        existingAccount.setBankName(updatedBankAccount.getBankName());
        existingAccount.setAccountNumber(updatedBankAccount.getAccountNumber());
        existingAccount.setAccountHolderName(updatedBankAccount.getAccountHolderName());
        existingAccount.setIfsc(updatedBankAccount.getIfsc());
        if (updatedBankAccount.getBranch() != null) {
            existingAccount.setBranch(updatedBankAccount.getBranch());
        }
        if (updatedBankAccount.getSignaturePath() != null) {
            existingAccount.setSignaturePath(updatedBankAccount.getSignaturePath());
        }

        return bankAccountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteBankAccount(Long id) {
        if (!bankAccountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bank Account not found with id: " + id);
        }
        bankAccountRepository.deleteById(id);
    }

    private void normalize(BankAccount bankAccount) {
        bankAccount.setBankName(trim(bankAccount.getBankName()));
        bankAccount.setAccountNumber(trim(bankAccount.getAccountNumber()));
        bankAccount.setAccountHolderName(trim(bankAccount.getAccountHolderName()));
        bankAccount.setIfsc(trim(bankAccount.getIfsc()));
        bankAccount.setBranch(trim(bankAccount.getBranch()));
        bankAccount.setSignaturePath(trim(bankAccount.getSignaturePath()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
