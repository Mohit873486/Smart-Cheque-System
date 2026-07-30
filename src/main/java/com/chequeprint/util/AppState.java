package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankAccount;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.LayoutField;
import com.chequeprint.service.BankService;
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

    private final BankService bankService = new BankService();

    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChanged();
    }

    private final List<StateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();

    private AppState() {
        // Automatically broadcast events to state listeners on any state change
        selectedBank.addListener((obs, o, n) -> notifyStateChangeListeners());
        selectedBankAccount.addListener((obs, o, n) -> notifyStateChangeListeners());
        selectedTemplate.addListener((obs, o, n) -> notifyStateChangeListeners());
        currentCheque.addListener((obs, o, n) -> notifyStateChangeListeners());
        selectedPrinter.addListener((obs, o, n) -> notifyStateChangeListeners());

        try {
            // Restore saved printer from Preferences if available
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

    /**
     * Registers a listener to be notified when bank, template, or cheque data changes.
     */
    public void addStateChangeListener(StateChangeListener listener) {
        if (listener != null && !stateChangeListeners.contains(listener)) {
            stateChangeListeners.add(listener);
        }
    }

    /**
     * Unregisters a state change listener.
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    /**
     * Broadcasts state change events to all subscribed UI controllers on the JavaFX thread.
     */
    public void notifyStateChangeListeners() {
        if (Platform.isFxApplicationThread()) {
            dispatchToListeners();
        } else {
            Platform.runLater(this::dispatchToListeners);
        }
    }

    private void dispatchToListeners() {
        for (StateChangeListener listener : stateChangeListeners) {
            try {
                listener.onStateChanged();
            } catch (Exception ex) {
                System.err.println("AppState event listener warning: " + ex.getMessage());
            }
        }
    }

    // --- Selected Bank ---
    public ObjectProperty<Bank> selectedBankProperty() {
        return selectedBank;
    }

    public Bank getSelectedBank() {
        return selectedBank.get();
    }

    public void setSelectedBank(Bank bank) {
        if (Objects.equals(this.selectedBank.get(), bank)) {
            return; // Skip redundant state change
        }
        this.selectedBank.set(bank);
        if (bank == null) {
            setSelectedTemplate(null);
            return;
        }
        if (bank.getId() != null) {
            Session.setSelectedBankId(bank.getId().longValue());
            fetchTemplateForBank(bank.getId().longValue(), bank.getBankCode());
        }
    }

    // --- Selected Bank Account ---
    public ObjectProperty<BankAccount> selectedBankAccountProperty() {
        return selectedBankAccount;
    }

    public BankAccount getSelectedBankAccount() {
        return selectedBankAccount.get();
    }

    public void setSelectedBankAccount(BankAccount account) {
        if (Objects.equals(this.selectedBankAccount.get(), account)) {
            return; // Skip redundant state change
        }
        this.selectedBankAccount.set(account);
        if (account == null) {
            setSelectedTemplate(null);
            return;
        }
        if (account.getId() != null) {
            Session.setSelectedBankId(account.getId().longValue());
            fetchTemplateForBank(account.getId().longValue(), account.getBankName());
        }
    }

    /**
     * Immediately clears current template (preventing stale preview rendering)
     * and fetches the updated cheque template from DB / API asynchronously.
     */
    private final Map<String, BankTemplateLayout> templateMemoryCache = new java.util.concurrent.ConcurrentHashMap<>();

    public void invalidateTemplateCache(Long bankId) {
        if (bankId != null) {
            templateMemoryCache.remove("tpl_" + bankId);
            templateMemoryCache.remove("acc_" + bankId);
        } else {
            templateMemoryCache.clear();
        }
    }

    private void fetchTemplateForBank(Long bankId, String bankCodeOrName) {
        // Prevent preview rendering until new template is loaded
        setSelectedTemplate(null);

        Long targetTemplateId = (getSelectedBankAccount() != null && getSelectedBankAccount().getTemplateId() != null)
                ? getSelectedBankAccount().getTemplateId()
                : bankId;

        String cacheKey = "tpl_" + targetTemplateId;
        if (templateMemoryCache.containsKey(cacheKey)) {
            setSelectedTemplate(templateMemoryCache.get(cacheKey));
            return;
        }

        new Thread(() -> {
            try {
                // 1. If active bank account has a specific active template, load it first from API
                if (targetTemplateId != null && targetTemplateId > 0) {
                    List<Map<String, Object>> fields = bankService.getTemplateFields(targetTemplateId);
                    if (fields != null && !fields.isEmpty()) {
                        BankTemplateLayout layout = new BankTemplateLayout();
                        for (Map<String, Object> map : fields) {
                            String name = (String) map.get("fieldName");
                            Object xObj = map.get("xPosition");
                            Object yObj = map.get("yPosition");
                            if (name != null && xObj instanceof Number && yObj instanceof Number) {
                                double x = ((Number) xObj).doubleValue();
                                double y = ((Number) yObj).doubleValue();
                                LayoutField field = unmapField(name);
                                if (field != null) {
                                    layout.setFieldPosition(field, x / 720.0, y / 300.0);
                                }
                            }
                        }
                        layout.ensureAllFields();
                        templateMemoryCache.put(cacheKey, layout);
                        Platform.runLater(() -> setSelectedTemplate(layout));
                        return;
                    }
                }

                // 2. Check local store cache fallback
                Map<String, BankTemplateLayout> allLayouts = bankService.loadAllLayouts();
                String codeKey = bankCodeOrName != null ? bankCodeOrName.trim().toUpperCase() : "";
                if (!codeKey.isEmpty() && allLayouts.containsKey(codeKey)) {
                    BankTemplateLayout cached = allLayouts.get(codeKey).copy();
                    templateMemoryCache.put(cacheKey, cached);
                    Platform.runLater(() -> setSelectedTemplate(cached));
                    return;
                }

                BankTemplateLayout fallback = new BankTemplateLayout();
                fallback.ensureAllFields();
                templateMemoryCache.put(cacheKey, fallback);
                Platform.runLater(() -> setSelectedTemplate(fallback));
            } catch (Exception ex) {
                System.err.println("AppState template fetch warning: " + ex.getMessage());
                BankTemplateLayout fallback = new BankTemplateLayout();
                fallback.ensureAllFields();
                Platform.runLater(() -> setSelectedTemplate(fallback));
            }
        }, "app-state-fetch-template").start();
    }

    private LayoutField unmapField(String name) {
        return switch (name.toLowerCase()) {
            case "name", "payee" -> LayoutField.PAYEE;
            case "amount", "amount_number" -> LayoutField.AMOUNT_NUMBER;
            case "amount_words" -> LayoutField.AMOUNT_WORDS;
            case "date" -> LayoutField.DATE;
            case "signature" -> LayoutField.SIGNATURE;
            case "logo" -> LayoutField.BANK_LOGO;
            case "micr" -> LayoutField.MICR;
            default -> null;
        };
    }

    // --- Selected Cheque Template Layout ---
    public ObjectProperty<BankTemplateLayout> selectedTemplateProperty() {
        return selectedTemplate;
    }

    public BankTemplateLayout getSelectedTemplate() {
        return selectedTemplate.get();
    }

    public BankTemplateLayout getCurrentTemplate() {
        return getSelectedTemplate();
    }

    public void setSelectedTemplate(BankTemplateLayout template) {
        if (Objects.equals(this.selectedTemplate.get(), template)) {
            return; // Skip redundant state change
        }
        System.out.println("[DEBUG AppState] selectedTemplate updated: " + (template != null ? "layout loaded (" + template.getWidthInches() + "x" + template.getHeightInches() + " in)" : "null"));
        this.selectedTemplate.set(template);
    }

    public void setCurrentTemplate(BankTemplateLayout template) {
        setSelectedTemplate(template);
    }

    // --- Current Active Cheque ---
    public ObjectProperty<Cheque> currentChequeProperty() {
        return currentCheque;
    }

    public Cheque getCurrentCheque() {
        return currentCheque.get();
    }

    public void setCurrentCheque(Cheque cheque) {
        if (Objects.equals(this.currentCheque.get(), cheque)) {
            return; // Skip redundant state change
        }
        System.out.println("[DEBUG AppState] currentCheque updated: " + (cheque != null ? ("payee='" + cheque.getPayeeName() + "', amount=" + cheque.getAmount()) : "null"));
        this.currentCheque.set(cheque);
    }

    // --- Active Selected Printer ---
    public ObjectProperty<Printer> selectedPrinterProperty() {
        return selectedPrinter;
    }

    public Printer getSelectedPrinter() {
        return selectedPrinter.get();
    }

    public void setSelectedPrinter(Printer printer) {
        if (Objects.equals(this.selectedPrinter.get(), printer)) {
            return;
        }
        System.out.println("[DEBUG AppState] selectedPrinter updated: " + (printer != null ? printer.getName() : "null"));
        this.selectedPrinter.set(printer);
        if (printer != null && printer.getName() != null) {
            try {
                if (PREFS != null) PREFS.put(PREF_PRINTER, printer.getName());
            } catch (Throwable ignored) {}
        }
    }

    public void setSelectedPrinterByName(String printerName) {
        if (printerName != null && !printerName.isBlank()) {
            try {
                if (PREFS != null) PREFS.put(PREF_PRINTER, printerName);
            } catch (Throwable ignored) {}
            Printer p = PrinterUtils.findPrinterByName(printerName);
            if (p != null) {
                setSelectedPrinter(p);
            }
        }
    }

    /**
     * Ensures Bank and Template are loaded in global AppState.
     * Prevents empty state when new windows or dialogs open.
     */
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

    /**
     * Clears global application state upon logout or session reset.
     */
    public void clear() {
        setSelectedBank(null);
        setSelectedBankAccount(null);
        setSelectedTemplate(null);
        setCurrentCheque(null);
        setSelectedPrinter(null);
    }
}
