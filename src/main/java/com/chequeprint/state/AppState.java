package com.chequeprint.state;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankAccount;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.LayoutField;
import com.chequeprint.model.User;
import com.chequeprint.service.BankService;
import com.chequeprint.util.ChequeSizeCodec;
import com.chequeprint.util.PrinterUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.Printer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

public final class AppState {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppState.class);
    private static final String PREF_PRINTER = "selected_printer";
    private static final String PREF_DEFAULT_PRINTER = "default_printer";
    private static final AppState INSTANCE = new AppState();

    private final ObjectProperty<Bank> selectedBank = new SimpleObjectProperty<>();
    private final ObjectProperty<BankAccount> selectedBankAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<BankTemplateLayout> selectedTemplate = new SimpleObjectProperty<>();
    private final ObjectProperty<Cheque> currentCheque = new SimpleObjectProperty<>();
    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObservableList<Printer> availablePrinters = FXCollections.observableArrayList();
    private final ObjectProperty<User> loggedInUser = new SimpleObjectProperty<>();

    private final BankService bankService = new BankService();
    private final Set<Long> templateLoadsInFlight = ConcurrentHashMap.newKeySet();

    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChanged();
    }

    private final List<StateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();

    private AppState() {
        selectedBank.addListener((obs, oldBank, newBank) -> {
            if (!Objects.equals(oldBank, newBank)) {
                notifyStateChangeListeners();
                if (newBank != null && newBank.getId() != null) {
                    fetchTemplateForBank(newBank.getId().longValue(), newBank.getBankCode());
                }
            }
        });

        selectedBankAccount.addListener((obs, o, n) -> { if (!Objects.equals(o, n)) notifyStateChangeListeners(); });
        selectedTemplate.addListener((obs, o, n) -> { if (!Objects.equals(o, n)) notifyStateChangeListeners(); });
        currentCheque.addListener((obs, o, n) -> { if (!Objects.equals(o, n)) notifyStateChangeListeners(); });
        selectedPrinter.addListener((obs, o, n) -> { if (!Objects.equals(o, n)) notifyStateChangeListeners(); });

        refreshAvailablePrinters();
        initializeDefaultPrinter();
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    public ObjectProperty<Bank> selectedBankProperty() {
        return selectedBank;
    }

    public Bank getSelectedBank() {
        return selectedBank.get();
    }

    public void setSelectedBank(Bank bank) {
        this.selectedBank.set(bank);
    }

    public ObjectProperty<BankAccount> selectedBankAccountProperty() {
        return selectedBankAccount;
    }

    public BankAccount getSelectedBankAccount() {
        return selectedBankAccount.get();
    }

    public void setSelectedBankAccount(BankAccount account) {
        this.selectedBankAccount.set(account);
    }

    public ObjectProperty<BankTemplateLayout> selectedTemplateProperty() {
        return selectedTemplate;
    }

    public BankTemplateLayout getSelectedTemplate() {
        return selectedTemplate.get();
    }

    public void setSelectedTemplate(BankTemplateLayout template) {
        this.selectedTemplate.set(template);
    }

    public ObjectProperty<Cheque> currentChequeProperty() {
        return currentCheque;
    }

    public Cheque getCurrentCheque() {
        return currentCheque.get();
    }

    public void setCurrentCheque(Cheque cheque) {
        Cheque current = this.currentCheque.get();
        if (Objects.equals(current, cheque)) {
            return;
        }
        if (current != null && cheque != null && Objects.equals(current.getId(), cheque.getId())) {
            return;
        }
        this.currentCheque.set(cheque);
    }

    public ObjectProperty<Cheque> currentChequeDataProperty() {
        return currentCheque;
    }

    public Cheque getCurrentChequeData() {
        return currentCheque.get();
    }

    public void setCurrentChequeData(Cheque cheque) {
        setCurrentCheque(cheque);
    }

    public ObjectProperty<Printer> selectedPrinterProperty() {
        return selectedPrinter;
    }

    public ObservableList<Printer> getAvailablePrinters() {
        return availablePrinters;
    }

    public void refreshAvailablePrinters() {
        List<Printer> printers = PrinterUtils.getAllAvailablePrinters();
        availablePrinters.setAll(printers);

        Printer currentPrinter = selectedPrinter.get();
        if (currentPrinter != null && !PrinterUtils.isValidPrinter(currentPrinter)) {
            selectedPrinter.set(null);
        }
    }

    public Printer getSelectedPrinter() {
        return selectedPrinter.get();
    }

    public void setSelectedPrinter(Printer printer) {
        if (printer != null && !PrinterUtils.isValidPrinter(printer)) {
            throw new IllegalArgumentException("Selected printer is invalid or unavailable.");
        }
        this.selectedPrinter.set(printer);
        if (PREFS != null) {
            try {
                if (printer != null && printer.getName() != null) {
                    PREFS.put(PREF_PRINTER, printer.getName());
                } else {
                    PREFS.remove(PREF_PRINTER);
                }
                PREFS.flush();
            } catch (Exception e) {
                System.err.println("[AppState] Failed to persist printer preference: " + e.getMessage());
            }
        }
    }

    public String getDefaultPrinterName() {
        if (PREFS == null) {
            return null;
        }
        String defaultPrinter = PREFS.get(PREF_DEFAULT_PRINTER, null);
        if (defaultPrinter == null || defaultPrinter.isBlank()) {
            defaultPrinter = PREFS.get(PREF_PRINTER, null);
        }
        return defaultPrinter;
    }

    public Printer getDefaultPrinter() {
        String defaultPrinterName = getDefaultPrinterName();
        return defaultPrinterName != null ? PrinterUtils.findPrinterByName(defaultPrinterName) : null;
    }

    public void setDefaultPrinter(Printer printer) {
        if (printer != null && !PrinterUtils.isValidPrinter(printer)) {
            throw new IllegalArgumentException("Default printer is invalid or unavailable.");
        }
        try {
            if (printer != null && printer.getName() != null) {
                PREFS.put(PREF_DEFAULT_PRINTER, printer.getName());
                PREFS.put(PREF_PRINTER, printer.getName());
                selectedPrinter.set(printer);
            } else {
                PREFS.remove(PREF_DEFAULT_PRINTER);
            }
            PREFS.flush();
        } catch (Exception e) {
            System.err.println("[AppState] Failed to persist default printer preference: " + e.getMessage());
        }
    }

    public ObjectProperty<User> loggedInUserProperty() {
        return loggedInUser;
    }

    public User getLoggedInUser() {
        return loggedInUser.get();
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser.set(user);
    }

    public void setSelectedPrinterByName(String printerName) {
        if (printerName == null || printerName.isBlank() || printerName.equalsIgnoreCase("None")) {
            setSelectedPrinter(null);
            return;
        }
        Printer p = PrinterUtils.findPrinterByName(printerName);
        if (p != null) {
            setSelectedPrinter(p);
        } else {
            System.err.println("[AppState] Printer not found on system: " + printerName);
        }
    }

    public void setDefaultPrinterByName(String printerName) {
        if (printerName == null || printerName.isBlank() || printerName.equalsIgnoreCase("None")) {
            setDefaultPrinter(null);
            return;
        }
        Printer p = PrinterUtils.findPrinterByName(printerName);
        if (p != null) {
            setDefaultPrinter(p);
        } else {
            System.err.println("[AppState] Default printer not found on system: " + printerName);
        }
    }

    public String getSelectedPrinterName() {
        Printer printer = getSelectedPrinter();
        return printer != null ? printer.getName() : null;
    }

    public Printer resolveSelectedOrDefaultPrinter() {
        Printer printer = getSelectedPrinter();
        if (PrinterUtils.isValidPrinter(printer)) {
            return printer;
        }
        printer = getDefaultPrinter();
        if (PrinterUtils.isValidPrinter(printer)) {
            selectedPrinter.set(printer);
            return printer;
        }
        return null;
    }

    public void initializeDefaultPrinter() {
        try {
            refreshAvailablePrinters();
            Printer printer = getDefaultPrinter();
            if (printer == null) {
                String savedPrinter = PREFS != null ? PREFS.get(PREF_PRINTER, null) : null;
                if (savedPrinter != null && !savedPrinter.isBlank()) {
                    printer = PrinterUtils.findPrinterByName(savedPrinter);
                }
            }
            if (printer != null) {
                selectedPrinter.set(printer);
                if (PREFS != null && printer.getName() != null) {
                    PREFS.put(PREF_DEFAULT_PRINTER, printer.getName());
                    PREFS.put(PREF_PRINTER, printer.getName());
                    PREFS.flush();
                }
                System.out.println("[AppState] Default printer initialized: " + printer.getName());
            } else {
                selectedPrinter.set(null);
                System.err.println("[AppState] No default printer selected or saved printer is unavailable.");
            }
        } catch (Throwable t) {
            System.err.println("[AppState] Warning during printer initialization: " + t.getMessage());
        }
    }

    public void addStateChangeListener(StateChangeListener listener) {
        if (listener != null && !stateChangeListeners.contains(listener)) {
            stateChangeListeners.add(listener);
        }
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        if (listener != null) {
            stateChangeListeners.remove(listener);
        }
    }

    private void notifyStateChangeListeners() {
        for (StateChangeListener listener : stateChangeListeners) {
            try {
                listener.onStateChanged();
            } catch (Exception e) {
                System.err.println("[AppState] Exception in state change listener: " + e.getMessage());
            }
        }
    }

    public void updateFieldPosition(LayoutField field, double xRatio, double yRatio) {
        BankTemplateLayout currentLayout = getSelectedTemplate();
        if (currentLayout == null) {
            currentLayout = new BankTemplateLayout();
        }
        currentLayout.setFieldPosition(field, xRatio, yRatio);
        setSelectedTemplate(currentLayout);
        notifyStateChangeListeners();
    }

    public void fetchTemplateForBank(Long bankId, String bankCode) {
        if (bankId == null) return;
        if (!templateLoadsInFlight.add(bankId)) {
            return;
        }
        new Thread(() -> {
            try {
                Bank b = bankService.getById(bankId.intValue());
                if (b != null) {
                    BankTemplateLayout layout = ChequeSizeCodec.decodeLayout(b.getChequeSize());
                    layout.ensureAllFields();
                    Platform.runLater(() -> setSelectedTemplate(layout));
                }
            } catch (Exception e) {
                System.err.println("[AppState] Failed to fetch template for bankId=" + bankId + ": " + e.getMessage());
            } finally {
                templateLoadsInFlight.remove(bankId);
            }
        }, "app-state-template-loader").start();
    }

    public void ensureBankAndTemplateLoaded() {
        if (getSelectedBank() == null || getSelectedTemplate() == null) {
            new Thread(() -> {
                try {
                    List<Bank> banks = bankService.getAll();
                    if (!banks.isEmpty()) {
                        Bank defaultBank = banks.get(0);
                        if (getSelectedBank() == null) {
                            Platform.runLater(() -> setSelectedBank(defaultBank));
                        }
                        if (getSelectedTemplate() == null && defaultBank.getId() != null) {
                            fetchTemplateForBank(defaultBank.getId().longValue(), defaultBank.getBankCode());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AppState] Fallback load error: " + e.getMessage());
                }
            }, "app-state-fallback-loader").start();
        }
    }

    public void clear() {
        setSelectedBank(null);
        setSelectedBankAccount(null);
        setSelectedTemplate(null);
        setCurrentCheque(null);
        setSelectedPrinter(null);
        setLoggedInUser(null);
    }
}
