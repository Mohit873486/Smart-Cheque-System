package com.chequeprint.service;

import com.chequeprint.state.AppState;
import javafx.collections.ObservableList;
import javafx.print.Printer;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated All printer management is now handled by {@link PrintService}.
 *             This class is retained only for binary compatibility.
 *             Migrate any direct callers to use PrintService instead.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class PrinterService {

    public enum PrinterRoutingMode { SINGLE, BULK }
    public enum PrinterType        { DEFAULT, OFFICE }

    private final PrintService delegate;

    public PrinterService()                 { this.delegate = new PrintService(); }
    PrinterService(AppState ignored)        { this.delegate = new PrintService(); }

    public List<Printer>          getAllPrinters()                               { return delegate.getAvailablePrinters(); }
    public List<Printer>          fetchAvailablePrinters()                      { return delegate.getAvailablePrinters(); }
    public List<Printer>          getAvailablePrinters()                        { return delegate.getAvailablePrinters(); }
    public ObservableList<Printer>refreshPrinters()                             { return delegate.refreshPrinters(); }
    public ObservableList<String> refreshPrinterNames()                         { return delegate.refreshPrinterNames(); }
    public Optional<Printer>      getSelectedPrinter()                          { return delegate.getSelectedPrinter(); }
    public Optional<Printer>      getDefaultPrinter()                           { return delegate.getDefaultPrinter(); }
    public Optional<Printer>      findPrinterByName(String name)                { return delegate.findPrinterByName(name); }
    public Optional<Printer>      selectPrinterByName(String name)              { return delegate.selectPrinterByName(name); }
    public Printer                selectPrinter(Printer p)                      { return delegate.selectPrinter(p); }
    public Printer                saveDefaultPrinter(Printer p)                 { return delegate.saveDefaultPrinter(p); }
    public Optional<Printer>      saveDefaultPrinterByName(String name)         { return delegate.selectPrinterByName(name); }
    public Optional<Printer>      resolveSelectedOrDefaultPrinter()             { return delegate.resolveSelectedOrDefaultPrinter(); }
    public Optional<Printer>      initializeSelectedPrinter()                   { return Optional.ofNullable(delegate.initializeDefaultPrinter()); }
    public PrinterService         setOfficePrinter(String name)                 { delegate.setOfficePrinter(name); return this; }
    public PrinterService         setDefaultPrinter(String name)                { delegate.setDefaultPrinter(name); return this; }
    public Optional<Printer>      resolvePrinterForMode(PrinterRoutingMode mode){ return delegate.resolvePrinterForMode(PrintService.PrinterRoutingMode.valueOf(mode.name())); }
    public boolean                hasPrinters()                                  { return delegate.hasPrinters(); }
}
