package com.chequeprint.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PrinterUtilsTest {

    @Test
    public void testIsValidPrinterNameFiltersVirtualPrinters() {
        // Virtual & Fax printers must be rejected
        assertFalse(PrinterUtils.isValidPrinterName("Fax"));
        assertFalse(PrinterUtils.isValidPrinterName("Microsoft Print to PDF"));
        assertFalse(PrinterUtils.isValidPrinterName("Microsoft XPS Document Writer"));
        assertFalse(PrinterUtils.isValidPrinterName("Send To OneNote 2016"));
        assertFalse(PrinterUtils.isValidPrinterName("CutePDF Writer"));
        assertFalse(PrinterUtils.isValidPrinterName("doPDF v11"));
        assertFalse(PrinterUtils.isValidPrinterName("Foxit Reader PDF Printer"));
        assertFalse(PrinterUtils.isValidPrinterName("Virtual Thermal Printer"));
        assertFalse(PrinterUtils.isValidPrinterName("Root Print Queue"));
        assertFalse(PrinterUtils.isValidPrinterName(null));
        assertFalse(PrinterUtils.isValidPrinterName(""));
        assertFalse(PrinterUtils.isValidPrinterName("   "));
    }

    @Test
    public void testIsValidPrinterNameAcceptsPhysicalPrinters() {
        // Physical printers must be accepted
        assertTrue(PrinterUtils.isValidPrinterName("HP LaserJet Pro M404dn"));
        assertTrue(PrinterUtils.isValidPrinterName("Epson EcoTank L3150"));
        assertTrue(PrinterUtils.isValidPrinterName("Canon PIXMA G3010"));
        assertTrue(PrinterUtils.isValidPrinterName("Brother HL-L2321D"));
        assertTrue(PrinterUtils.isValidPrinterName("TVS HD 245 Gold Passbook Printer"));
    }

    @Test
    public void testAppStateRejectsVirtualPrinter() {
        AppState.getInstance().setSelectedPrinterByName("Fax");
        // Must be null or blocked when attempting to set a Fax printer
        assertNull(AppState.getInstance().getSelectedPrinter());
    }
}
