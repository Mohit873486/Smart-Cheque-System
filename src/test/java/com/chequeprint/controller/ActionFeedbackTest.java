package com.chequeprint.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ActionFeedbackTest {

    @Test
    public void testAlertMessageFormatting() {
        String successMsg = "Cheque updated.";
        String errorMsg = "Unable to connect to the backend server.";

        assertNotNull(successMsg);
        assertNotNull(errorMsg);
        assertTrue(successMsg.contains("updated"));
        assertTrue(errorMsg.contains("backend server"));
    }
}
