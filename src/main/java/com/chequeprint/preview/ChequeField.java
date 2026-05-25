package com.chequeprint.preview;

public enum ChequeField {
  PAYEE_NAME("Payee Name"),
  DATE("Date"),
  AMOUNT_NUMBER("Amount In Numbers"),
  AMOUNT_WORDS("Amount In Words");

  private final String displayName;

  ChequeField(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
