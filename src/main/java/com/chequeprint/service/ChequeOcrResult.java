package com.chequeprint.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ChequeOcrResult {
    private final String payeeName;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String chequeNumber;
    private final String rawText;

    public ChequeOcrResult(String payeeName, BigDecimal amount, LocalDate date, String chequeNumber, String rawText) {
        this.payeeName = payeeName;
        this.amount = amount;
        this.date = date;
        this.chequeNumber = chequeNumber;
        this.rawText = rawText;
    }

    public String getPayeeName() { return payeeName; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getChequeNumber() { return chequeNumber; }
    public String getRawText() { return rawText; }
}
