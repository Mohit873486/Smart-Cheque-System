package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import javafx.print.Printer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AppStatePersistenceTest {

    @BeforeEach
    public void setUp() {
        // Clear state before test
        AppState.getInstance().clear();
    }

    @Test
    public void testAppStateStoresAndPersistsBankAndTemplate() {
        Bank testBank = new Bank(10, "State Bank of India", "SBI", 8.0, 3.66, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        BankTemplateLayout testLayout = new BankTemplateLayout();
        testLayout.setBankCode("SBI");
        testLayout.setWidthInches(8.0);
        testLayout.setHeightInches(3.66);

        // Store in global AppState
        AppState.getInstance().setSelectedBank(testBank);
        AppState.getInstance().setSelectedTemplate(testLayout);

        // Verify global singleton retains stored data across calls
        assertEquals(testBank, AppState.getInstance().getSelectedBank());
        assertEquals(testLayout, AppState.getInstance().getSelectedTemplate());
    }

    @Test
    public void testAppStateNotifiesListenersOnPrinterChange() {
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        AppState.StateChangeListener listener = () -> listenerFired.set(true);

        AppState.getInstance().addStateChangeListener(listener);

        Printer validPrinter = PrinterUtils.getDefaultValidPrinter();
        if (validPrinter != null) {
            AppState.getInstance().setSelectedPrinter(validPrinter);
            assertTrue(listenerFired.get(), "State listener should be notified when printer changes.");
        }

        AppState.getInstance().removeStateChangeListener(listener);
    }

    @Test
    public void testAppStateRejectsNullOverwritesForInvalidPrinters() {
        Printer validPrinter = PrinterUtils.getDefaultValidPrinter();
        if (validPrinter != null) {
            AppState.getInstance().setSelectedPrinter(validPrinter);
            Printer initial = AppState.getInstance().getSelectedPrinter();

            // Attempting to set Fax printer should reject and set null
            AppState.getInstance().setSelectedPrinterByName("Fax");
            assertNull(AppState.getInstance().getSelectedPrinter());
        }
    }
}
