package com.chequeprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrinterRoutingServiceTest {

    @Test
    void bulkRoutingPrefersConfiguredOfficePrinter() {
        String selected = "Selected Printer";
        String office = "Office Printer";
        String defaultPrinter = "Default Printer";

        String routed = PrinterService.routePrinterName(
                PrinterService.PrinterRoutingMode.BULK,
                selected,
                defaultPrinter,
                office);

        assertEquals(office, routed);
    }

    @Test
    void singleRoutingFallsBackToDefaultPrinter() {
        String selected = "Selected Printer";
        String office = "Office Printer";
        String defaultPrinter = "Default Printer";

        String routed = PrinterService.routePrinterName(
                PrinterService.PrinterRoutingMode.SINGLE,
                selected,
                defaultPrinter,
                office);

        assertEquals(defaultPrinter, routed);
    }
}
