package com.chequeprint.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName = "ChequePro";

    @Column(length = 50)
    private String currency = "INR";

    @Column(name = "date_format", length = 50)
    private String dateFormat = "dd-MM-yyyy";

    @Column(length = 50)
    private String language = "English";

    @Column(name = "cheque_prefix", length = 20)
    private String chequePrefix = "CHQ";

    @Column(name = "default_bank", length = 100)
    private String defaultBank;

    @Column(name = "auto_print")
    private boolean autoPrint = false;

    @Column(name = "amount_confirm")
    private boolean amountConfirm = true;

    @Column(name = "invoice_prefix", length = 20)
    private String invoicePrefix = "INV";

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms = "Net 30";

    @Column(name = "auto_gst")
    private boolean autoGST = true;

    @Column(length = 20)
    private String theme = "light";

    public Settings() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getChequePrefix() { return chequePrefix; }
    public void setChequePrefix(String chequePrefix) { this.chequePrefix = chequePrefix; }

    public String getDefaultBank() { return defaultBank; }
    public void setDefaultBank(String defaultBank) { this.defaultBank = defaultBank; }

    public boolean isAutoPrint() { return autoPrint; }
    public void setAutoPrint(boolean autoPrint) { this.autoPrint = autoPrint; }

    public boolean isAmountConfirm() { return amountConfirm; }
    public void setAmountConfirm(boolean amountConfirm) { this.amountConfirm = amountConfirm; }

    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public boolean isAutoGST() { return autoGST; }
    public void setAutoGST(boolean autoGST) { this.autoGST = autoGST; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
