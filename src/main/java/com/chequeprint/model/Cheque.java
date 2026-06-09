package com.chequeprint.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Domain model for a cheque record. */
public class Cheque {

    public enum Status {
        Draft,
        Pending,
        Deposited,
        Cleared,
        Bounced,
        // legacy values retained for compatibility
        Approved,
        Rejected,
        Printed,
        Cancelled
    }

    private int id;
    private String chequeNo;
    private String payeeName;
    private BigDecimal amount;
    private String amountWords;
    private int bankId;
    private String bankName; // joined field for display
    private LocalDate issueDate;
    private Status status;

    public Cheque() {
    }

    public Cheque(String chequeNo, String payeeName, BigDecimal amount,
            int bankId, LocalDate issueDate) {
        this.chequeNo = chequeNo;
        this.payeeName = payeeName;
        this.amount = amount;
        this.bankId = bankId;
        this.issueDate = issueDate;
        this.status = Status.Draft;
    }

    // ---- Getters & Setters ----
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAmountWords() {
        return amountWords;
    }

    public void setAmountWords(String w) {
        this.amountWords = w;
    }

    public int getBankId() {
        return bankId;
    }

    public void setBankId(int bankId) {
        this.bankId = bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate d) {
        this.issueDate = d;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Cheque{" + chequeNo + ", " + payeeName + ", ₹" + amount + ", " + status + "}";
    }
}