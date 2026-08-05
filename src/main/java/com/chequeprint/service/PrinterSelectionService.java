package com.chequeprint.service;

import javafx.print.Printer;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated Merged into {@link PrintService}. Use PrintService directly.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class PrinterSelectionService {

    private final PrintService delegate = new PrintService();

    public List<Printer>   listAvailablePrinters()           { return delegate.getAvailablePrinters(); }
    public void            refreshAvailablePrinters()         { delegate.refreshPrinters(); }
    public Optional<Printer> findPrinter(String name)        { return delegate.findPrinterByName(name); }
    public Printer         selectPrinter(Printer p)           { return delegate.selectPrinter(p); }
    public Printer         selectPrinter(String name)         { return delegate.selectPrinterByNameOrThrow(name); }
    public Printer         initializeDefaultPrinter()         { return delegate.initializeDefaultPrinter(); }
    public Printer         getDefaultPrinter()                { return delegate.getDefaultPrinter().orElse(null); }
    public Printer         setDefaultPrinter(Printer p)       { return delegate.saveDefaultPrinter(p); }
    public Printer         resolveSelectedOrDefaultPrinter()  { return delegate.resolveSelectedOrDefaultPrinter().orElse(null); }
}
