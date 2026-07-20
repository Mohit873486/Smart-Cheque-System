package com.chequeprint.backend.service;

import com.chequeprint.backend.entity.BankAccount;
import com.chequeprint.backend.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public Optional<BankAccount> getBankAccountById(Long id) {
        return bankAccountRepository.findById(id);
    }

    public BankAccount createBankAccount(BankAccount bankAccount) {
        return bankAccountRepository.save(bankAccount);
    }

    public BankAccount updateBankAccount(Long id, BankAccount updatedBankAccount) {
        return bankAccountRepository.findById(id).map(bankAccount -> {
            bankAccount.setBankName(updatedBankAccount.getBankName());
            bankAccount.setAccountNumber(updatedBankAccount.getAccountNumber());
            bankAccount.setIfsc(updatedBankAccount.getIfsc());
            bankAccount.setBranch(updatedBankAccount.getBranch());
            bankAccount.setSignaturePath(updatedBankAccount.getSignaturePath());
            return bankAccountRepository.save(bankAccount);
        }).orElseThrow(() -> new RuntimeException("Bank Account not found with id " + id));
    }

    public void deleteBankAccount(Long id) {
        bankAccountRepository.deleteById(id);
    }
}
