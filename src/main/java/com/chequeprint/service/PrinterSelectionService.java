package com.chequeprint.service;

import com.chequeprint.state.AppState;
import com.chequeprint.util.PrinterUtils;
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

    public List<Printer> listAvailablePrinters() {
        return appState.getAvailablePrinters();
    }

    public void refreshAvailablePrinters() {
        appState.refreshAvailablePrinters();
    }

    public Optional<Printer> findPrinter(String printerName) {
        if (printerName == null || printerName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PrinterUtils.findPrinterByName(printerName));
    }

    public Printer selectPrinter(Printer printer) {
        if (printer == null) {
            throw new IllegalArgumentException("Printer must not be null.");
        }
        if (!PrinterUtils.isValidPrinter(printer)) {
            throw new IllegalArgumentException("Selected printer is invalid or unavailable.");
        }
        appState.setSelectedPrinter(printer);
        return printer;
    }

    public Printer selectPrinter(String printerName) {
        Printer printer = PrinterUtils.findPrinterByName(printerName);
        if (printer == null) {
            throw new IllegalArgumentException("Printer not found: " + printerName);
        }
        return selectPrinter(printer);
    }

    public Printer initializeDefaultPrinter() {
        appState.initializeDefaultPrinter();
        return appState.getSelectedPrinter();
    }

    public Printer getDefaultPrinter() {
        return appState.getSelectedPrinter();
    }
}
