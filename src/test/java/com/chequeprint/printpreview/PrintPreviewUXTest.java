package com.chequeprint.printpreview;

import com.chequeprint.util.AppState;
import com.chequeprint.util.PrinterUtils;
import javafx.print.Printer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrintPreviewUXTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testValidPrinterNamesListIsNotEmpty() {
        List<String> validPrinterNames = PrinterUtils.getValidPrinterNames();
        assertNotNull(validPrinterNames, "Valid printer names list must not be null.");
    }

    @Test
    public void testSettingDefaultPrinterByNameUpdatesAppState() {
        List<String> validNames = PrinterUtils.getValidPrinterNames();
        if (!validNames.isEmpty()) {
            String targetPrinterName = validNames.get(0);
            AppState.getInstance().setSelectedPrinterByName(targetPrinterName);

            Printer current = AppState.getInstance().getSelectedPrinter();
            assertNotNull(current, "Selected printer in AppState must not be null after setting by name.");
            assertEquals(targetPrinterName, current.getName());
        }
    }
}
