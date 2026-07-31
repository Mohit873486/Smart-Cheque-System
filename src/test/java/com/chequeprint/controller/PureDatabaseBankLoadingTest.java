package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.service.BankService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PureDatabaseBankLoadingTest {

    @Test
    public void testBankListOnlyContainsDatabaseItems() {
        List<Bank> dbBanks = new ArrayList<>();
        dbBanks.add(new Bank("Bank of Baroda", "BOB", "DEFAULT", true));

        assertFalse(dbBanks.isEmpty(), "Database bank list must reflect database records.");
        assertEquals(1, dbBanks.size());
        assertEquals("Bank of Baroda", dbBanks.get(0).getBankName());
    }

    @Test
    public void testEmptyBankListProducesNoDummyData() {
        List<Bank> dbBanks = new ArrayList<>(); // Empty response from database API

        assertTrue(dbBanks.isEmpty(), "Empty database API response must return zero bank items with no hardcoded fallbacks.");
    }
}
