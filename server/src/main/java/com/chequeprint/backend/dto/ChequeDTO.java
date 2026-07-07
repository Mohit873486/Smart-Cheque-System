package com.chequeprint.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ChequeDTO {
    private int id;
    private String chequeNo;
    private String payeeName;
    private BigDecimal amount;
    private String amountWords;
    private int bankId;
    private int accountId;
    private LocalDate issueDate;
    private String status;
    private String bankName;

    public ChequeDTO() {}

    public ChequeDTO(int id, String chequeNo, String payeeName, BigDecimal amount, String amountWords, int bankId, int accountId, LocalDate issueDate, String status, String bankName) {
        this.id = id;
        this.chequeNo = chequeNo;
        this.payeeName = payeeName;
        this.amount = amount;
        this.amountWords = amountWords;
        this.bankId = bankId;
        this.accountId = accountId;
        this.issueDate = issueDate;
        this.status = status;
        this.bankName = bankName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAmountWords() { return amountWords; }
    public void setAmountWords(String amountWords) { this.amountWords = amountWords; }

    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}
