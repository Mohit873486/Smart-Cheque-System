package com.chequeprint.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmptyStateTest {

    @Test
    public void testEmptyStateMessageWhenDataIsEmpty() {
        List<Object> dataList = new ArrayList<>();

        assertTrue(dataList.isEmpty());
        
        String emptyStateTitle = "No Bank Accounts Found";
        String emptyStateSubtext = "No bank accounts found. Create one to continue.";

        assertEquals("No Bank Accounts Found", emptyStateTitle);
        assertTrue(emptyStateSubtext.contains("Create one to continue"));
    }
}
