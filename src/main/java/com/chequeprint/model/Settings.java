package com.chequeprint.model;

import java.io.Serializable;

public class Settings implements Serializable{

  private int id;
  private String appName;
  private String currency;
  private String dateFormat;
  private String language;
  private String chequePrefix;
  private String invoicePrefix;
  private String theme;
  private boolean autoPrint;
  private boolean amountConfirm;
  private boolean autoGST;
  private String paymentTerms;  private String defaultBank;

  // 🔹 Default Constructor (IMPORTANT)
  public Settings() {
  }

  // 🔹 Parameterized Constructor
  public Settings(String appName, String currency, String dateFormat,
      String language, String chequePrefix, String defaultBank,
      boolean autoPrint, boolean amountConfirm, String invoicePrefix,
      String paymentTerms, boolean autoGST, String theme) {
    this.appName = appName;
    this.currency = currency;
    this.dateFormat = dateFormat;
    this.language = language;
    this.chequePrefix = chequePrefix;
    this.defaultBank = defaultBank;
    this.autoPrint = autoPrint;
    this.amountConfirm = amountConfirm;
    this.invoicePrefix = invoicePrefix;
    this.paymentTerms = paymentTerms;
    this.autoGST = autoGST;
    this.theme = theme;
  }

  // 🔹 Getters & Setters

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getChequePrefix() {
    return chequePrefix;
  }

  public void setChequePrefix(String chequePrefix) {
    this.chequePrefix = chequePrefix;
  }

  public String getInvoicePrefix() {
    return invoicePrefix;
  }

  public void setInvoicePrefix(String invoicePrefix) {
    this.invoicePrefix = invoicePrefix;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getDefaultBank() {
    return defaultBank;
  }

  public void setDefaultBank(String defaultBank) {
    this.defaultBank = defaultBank;
  }

  public boolean isAutoPrint() {
    return autoPrint;
  }

  public void setAutoPrint(boolean autoPrint) {
    this.autoPrint = autoPrint;
  }

  public boolean isAmountConfirm() {
    return amountConfirm;
  }

  public void setAmountConfirm(boolean amountConfirm) {
    this.amountConfirm = amountConfirm;
  }

  public boolean isAutoGST() {
    return autoGST;
  }

  public void setAutoGST(boolean autoGST) {
    this.autoGST = autoGST;
  }

  public String getPaymentTerms() {
    return paymentTerms;
  }

  public void setPaymentTerms(String paymentTerms) {
    this.paymentTerms = paymentTerms;
  }
}