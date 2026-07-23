package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.BankAccount;
import com.chequeprint.backend.exception.ResourceNotFoundException;
import com.chequeprint.backend.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Autowired
    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount getBankAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Account not found with id: " + id));
    }

    public BankAccount createBankAccount(BankAccount bankAccount) {
        if (bankAccountRepository.existsByAccountNumber(bankAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Bank Account with account number " + bankAccount.getAccountNumber() + " already exists.");
        }
        return bankAccountRepository.save(bankAccount);
    }

    public BankAccount updateBankAccount(Long id, BankAccount updatedBankAccount) {
        BankAccount existingAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Account not found with id: " + id));

        if (bankAccountRepository.existsByAccountNumberAndIdNot(updatedBankAccount.getAccountNumber(), id)) {
            throw new IllegalArgumentException("Bank Account with account number " + updatedBankAccount.getAccountNumber() + " already exists.");
        }

        existingAccount.setBankName(updatedBankAccount.getBankName());
        existingAccount.setAccountNumber(updatedBankAccount.getAccountNumber());
        existingAccount.setIfsc(updatedBankAccount.getIfsc());
        if (updatedBankAccount.getBranch() != null) {
            existingAccount.setBranch(updatedBankAccount.getBranch());
        }
        if (updatedBankAccount.getSignaturePath() != null) {
            existingAccount.setSignaturePath(updatedBankAccount.getSignaturePath());
        }

        return bankAccountRepository.save(existingAccount);
    }

    public void deleteBankAccount(Long id) {
        if (!bankAccountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bank Account not found with id: " + id);
        }
        bankAccountRepository.deleteById(id);
    }
}
