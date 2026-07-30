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
import javafx.print.Printer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

public final class AppState {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppState.class);
    private static final String PREF_PRINTER = "selected_printer";
    private static final AppState INSTANCE = new AppState();

    private final ObjectProperty<Bank> selectedBank = new SimpleObjectProperty<>();
    private final ObjectProperty<BankAccount> selectedBankAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<BankTemplateLayout> selectedTemplate = new SimpleObjectProperty<>();
    private final ObjectProperty<Cheque> currentCheque = new SimpleObjectProperty<>();
    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObjectProperty<User> loggedInUser = new SimpleObjectProperty<>();

    private final BankService bankService = new BankService();

    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChanged();
    }

    private final List<StateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();

    private AppState() {
        selectedBank.addListener((obs, oldBank, newBank) -> {
            notifyStateChangeListeners();
            if (newBank != null && newBank.getId() != null) {
                fetchTemplateForBank(newBank.getId().longValue(), newBank.getBankCode());
            }
        });

        selectedBankAccount.addListener((obs, o, n) -> notifyStateChangeListeners());
        selectedTemplate.addListener((obs, o, n) -> notifyStateChangeListeners());
        currentCheque.addListener((obs, o, n) -> notifyStateChangeListeners());
        selectedPrinter.addListener((obs, o, n) -> notifyStateChangeListeners());

        try {
            String savedPrinter = PREFS != null ? PREFS.get(PREF_PRINTER, null) : null;
            if (savedPrinter != null && !savedPrinter.isBlank()) {
                Printer p = PrinterUtils.findPrinterByName(savedPrinter);
                if (p != null) {
                    this.selectedPrinter.set(p);
                }
            }
        } catch (Throwable t) {
            System.err.println("[AppState] Warning during printer initialization: " + t.getMessage());
        }
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
        this.currentCheque.set(cheque);
    }

    public ObjectProperty<Printer> selectedPrinterProperty() {
        return selectedPrinter;
    }

    public Printer getSelectedPrinter() {
        return selectedPrinter.get();
    }

    public void setSelectedPrinter(Printer printer) {
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
