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
  private String paymentTerms;

  // 🔹 Default Constructor (IMPORTANT)
  public Settings() {
  }

  // 🔹 Parameterized Constructor
  public Settings(String appName, String currency, String dateFormat,
      String language, String chequePrefix,
      String invoicePrefix, String theme) {
    this.appName = appName;
    this.currency = currency;
    this.dateFormat = dateFormat;
    this.language = language;
    this.chequePrefix = chequePrefix;
    this.invoicePrefix = invoicePrefix;
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
}