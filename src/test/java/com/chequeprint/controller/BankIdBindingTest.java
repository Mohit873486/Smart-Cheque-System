package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BankIdBindingTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testBankIdAndBankNameBindingOnCheque() {
        Bank bank = new Bank("Bank of Baroda", "BOB", "DEFAULT", true);
        bank.setId(5);

        Cheque cheque = new Cheque();
        cheque.setPayeeName("Global Tech Pvt Ltd");
        cheque.setAmount(new BigDecimal("55000.00"));
        cheque.setIssueDate(LocalDate.now());
        cheque.setBankId(bank.getId());
        cheque.setBankName(bank.getBankName());

        AppState.getInstance().setCurrentChequeData(cheque);
        AppState.getInstance().setSelectedBank(bank);

        Cheque current = AppState.getInstance().getCurrentChequeData();
        assertNotNull(current, "Current cheque data must not be null.");
        assertEquals(5, current.getBankId().intValue(), "Bank ID must match database primary key ID 5.");
        assertEquals("Bank of Baroda", current.getBankName(), "Bank Name must match Bank of Baroda.");
    }
}
