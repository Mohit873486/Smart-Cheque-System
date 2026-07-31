package com.chequeprint.controller;

import com.chequeprint.model.BankAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankAccountCreationTest {

    @Test
    public void testBankAccountFieldValidation() {
        BankAccount account = new BankAccount();
        account.setBankName("Bank of Baroda");
        account.setAccountNumber("1234567890");
        account.setAccountHolderName("Acme Corp");
        account.setIfsc("BARB0MAINXX");
        account.setBranch("Main Branch");

        assertNotNull(account.getBankName());
        assertNotNull(account.getAccountNumber());
        assertNotNull(account.getAccountHolderName());
        assertNotNull(account.getIfscCode());
        assertEquals("Bank of Baroda", account.getBankName());
    }
}
