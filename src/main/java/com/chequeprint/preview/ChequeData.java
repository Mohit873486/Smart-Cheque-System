package com.chequeprint.preview;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ChequeData {
  private String payeeName;
  private String amountNumber;
  private String amountWords;
  private LocalDate issueDate;

  public ChequeData() {
    this("", "", "", LocalDate.now());
  }

  public ChequeData(String payeeName, String amountNumber, String amountWords, LocalDate issueDate) {
    this.payeeName = Objects.requireNonNullElse(payeeName, "");
    this.amountNumber = Objects.requireNonNullElse(amountNumber, "");
    this.amountWords = Objects.requireNonNullElse(amountWords, "");
    this.issueDate = issueDate != null ? issueDate : LocalDate.now();
  }

  public String getPayeeName() {
    return payeeName;
  }

  public void setPayeeName(String payeeName) {
    this.payeeName = Objects.requireNonNullElse(payeeName, "");
  }

  public String getAmountNumber() {
    return amountNumber;
  }

  public void setAmountNumber(String amountNumber) {
    this.amountNumber = Objects.requireNonNullElse(amountNumber, "");
  }

  public String getAmountWords() {
    return amountWords;
  }

  public void setAmountWords(String amountWords) {
    this.amountWords = Objects.requireNonNullElse(amountWords, "");
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate != null ? issueDate : LocalDate.now();
  }

  public String getIssueDateText() {
    return issueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
  }
}
