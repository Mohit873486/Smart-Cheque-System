package com.chequeprint.service;

import com.chequeprint.state.AppState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.Printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized JavaFX printer discovery and selection service.
 */
public class PrinterService {

    public enum PrinterRoutingMode {
        SINGLE,
        BULK
    }

    public enum PrinterType {
        DEFAULT,
        OFFICE
    }

    private final AppState appState;
    private final Map<PrinterType, String> printerTypeConfiguration = new EnumMap<>(PrinterType.class);

    public PrinterService() {
        this(AppState.getInstance());
    }

    PrinterService(AppState appState) {
        this.appState = appState;
    }

    public List<Printer> fetchAvailablePrinters() {
        Map<String, Printer> printersByName = new LinkedHashMap<>();
        try {
            for (Printer printer : Printer.getAllPrinters()) {
                if (isUsablePrinter(printer)) {
                    printersByName.putIfAbsent(printer.getName(), printer);
                }
            }
        } catch (Throwable t) {
            System.err.println("[PrinterService] Failed to query JavaFX printers: " + t.getMessage());
        }
        return new ArrayList<>(printersByName.values());
    }

    public ObservableList<Printer> refreshPrinters() {
        List<Printer> printers = fetchAvailablePrinters();
        appState.getAvailablePrinters().setAll(printers);

        Printer selectedPrinter = appState.getSelectedPrinter();
        if (selectedPrinter != null && findByName(printers, selectedPrinter.getName()).isEmpty()) {
            appState.setSelectedPrinter(null);
        }

        return appState.getAvailablePrinters();
    }

    public ObservableList<String> refreshPrinterNames() {
        ObservableList<String> names = FXCollections.observableArrayList();
        for (Printer printer : refreshPrinters()) {
            names.add(printer.getName());
        }
        return names;
    }

    public List<Printer> getAvailablePrinters() {
        return Collections.unmodifiableList(appState.getAvailablePrinters());
    }

    public Optional<Printer> getSelectedPrinter() {
        Printer selectedPrinter = appState.getSelectedPrinter();
        if (selectedPrinter == null) {
            return Optional.empty();
        }
        return findByName(appState.getAvailablePrinters(), selectedPrinter.getName());
    }

    public Optional<Printer> getDefaultPrinter() {
        Printer defaultPrinter = appState.getDefaultPrinter();
        if (defaultPrinter == null) {
            return Optional.empty();
        }
        return findByName(refreshPrinters(), defaultPrinter.getName());
    }

    public Optional<Printer> findPrinterByName(String printerName) {
        if (printerName == null || printerName.isBlank()) {
            return Optional.empty();
        }
        return findByName(refreshPrinters(), printerName);
    }

    public Optional<Printer> selectPrinterByName(String printerName) {
        Optional<Printer> printer = findPrinterByName(printerName);
        printer.ifPresent(appState::setSelectedPrinter);
        return printer;
    }

    public Printer selectPrinter(Printer printer) {
        if (!isUsablePrinter(printer)) {
            throw new IllegalArgumentException("Selected printer is invalid or unavailable.");
        }
        Optional<Printer> detectedPrinter = findByName(refreshPrinters(), printer.getName());
        Printer selectedPrinter = detectedPrinter.orElseThrow(
                () -> new IllegalArgumentException("Printer not found: " + printer.getName()));
        appState.setSelectedPrinter(selectedPrinter);
        return selectedPrinter;
    }

    public Printer saveDefaultPrinter(Printer printer) {
        Printer selectedPrinter = selectPrinter(printer);
        appState.setDefaultPrinter(selectedPrinter);
        return selectedPrinter;
    }

    public Optional<Printer> saveDefaultPrinterByName(String printerName) {
        Optional<Printer> printer = findPrinterByName(printerName);
        printer.ifPresent(appState::setDefaultPrinter);
        return printer;
    }

    public Optional<Printer> resolveSelectedOrDefaultPrinter() {
        refreshPrinters();
        Printer printer = appState.resolveSelectedOrDefaultPrinter();
        if (printer == null) {
            return Optional.empty();
        }
        return findByName(appState.getAvailablePrinters(), printer.getName());
    }

    public PrinterService configurePrinterType(PrinterType type, String printerName) {
        if (type == null) {
            throw new IllegalArgumentException("Printer type must not be null.");
        }
        if (printerName == null || printerName.isBlank()) {
            printerTypeConfiguration.remove(type);
            return this;
        }
        printerTypeConfiguration.put(type, printerName.trim());
        return this;
    }

    public Optional<String> getConfiguredPrinterName(PrinterType type) {
        if (type == null) {
            return Optional.empty();
        }
        String configuredName = printerTypeConfiguration.get(type);
        return Optional.ofNullable(configuredName).filter(name -> !name.isBlank());
    }

    public Optional<Printer> getConfiguredPrinter(PrinterType type) {
        Optional<String> configuredName = getConfiguredPrinterName(type);
        if (configuredName.isEmpty()) {
            return Optional.empty();
        }
        return findPrinterByName(configuredName.get());
    }

    public Optional<Printer> resolvePrinterForMode(PrinterRoutingMode mode) {
        refreshPrinters();

        if (mode == PrinterRoutingMode.BULK) {
            return getConfiguredPrinter(PrinterType.OFFICE)
                    .or(() -> getDefaultPrinter())
                    .or(() -> resolveSelectedOrDefaultPrinter());
        }

        return getDefaultPrinter()
                .or(() -> resolveSelectedOrDefaultPrinter());
    }

    public static String routePrinterName(
            PrinterRoutingMode mode,
            String selectedPrinterName,
            String defaultPrinterName,
            String officePrinterName) {

        if (mode == PrinterRoutingMode.BULK) {
            if (officePrinterName != null && !officePrinterName.isBlank()) {
                return officePrinterName;
            }
        }

        if (defaultPrinterName != null && !defaultPrinterName.isBlank()) {
            return defaultPrinterName;
        }

        return selectedPrinterName;
    }

    public Optional<Printer> initializeSelectedPrinter() {
        ObservableList<Printer> printers = refreshPrinters();
        if (printers.isEmpty()) {
            appState.setSelectedPrinter(null);
            return Optional.empty();
        }

        Optional<Printer> selectedPrinter = getSelectedPrinter();
        if (selectedPrinter.isPresent()) {
            return selectedPrinter;
        }
        Optional<Printer> defaultPrinter = getDefaultPrinter();
        defaultPrinter.ifPresent(appState::setSelectedPrinter);
        return defaultPrinter;
    }

    public boolean hasPrinters() {
        return !appState.getAvailablePrinters().isEmpty();
    }

    private Optional<Printer> findByName(List<Printer> printers, String printerName) {
        if (printerName == null || printerName.isBlank()) {
            return Optional.empty();
        }
        return printers.stream()
                .filter(printer -> printer.getName().equalsIgnoreCase(printerName.trim()))
                .findFirst();
    }

    private boolean isUsablePrinter(Printer printer) {
        return printer != null && printer.getName() != null && !printer.getName().isBlank();
    }
}
