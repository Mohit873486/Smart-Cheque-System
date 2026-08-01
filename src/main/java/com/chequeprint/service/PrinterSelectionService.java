package com.chequeprint.service;

import com.chequeprint.state.AppState;
import javafx.print.Printer;

import java.util.List;
import java.util.Optional;

/**
 * Controller -> Service -> State printer selection flow.
 *
 * This service is the single entry point for UI code that needs to list,
 * select, or restore the active JavaFX printer. It keeps the controller thin
 * and ensures printer choice is persisted in AppState.
 */
public class PrinterSelectionService {

    private final AppState appState = AppState.getInstance();
    private final PrinterService printerService = new PrinterService();

    public List<Printer> listAvailablePrinters() {
        return printerService.getAvailablePrinters();
    }

    public void refreshAvailablePrinters() {
        printerService.refreshPrinters();
    }

    public Optional<Printer> findPrinter(String printerName) {
        return printerService.findPrinterByName(printerName);
    }

    public Printer selectPrinter(Printer printer) {
        return printerService.selectPrinter(printer);
    }

    public Printer selectPrinter(String printerName) {
        return printerService.selectPrinterByName(printerName)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerName));
    }

    public Printer initializeDefaultPrinter() {
        return printerService.initializeSelectedPrinter().orElse(null);
    }

    public Printer getDefaultPrinter() {
        return printerService.getDefaultPrinter().orElse(null);
    }

    public Printer setDefaultPrinter(Printer printer) {
        return printerService.saveDefaultPrinter(printer);
    }

    public Printer resolveSelectedOrDefaultPrinter() {
        return printerService.resolveSelectedOrDefaultPrinter().orElse(null);
    }
}
