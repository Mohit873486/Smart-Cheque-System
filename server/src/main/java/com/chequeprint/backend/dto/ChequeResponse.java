package com.chequeprint.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ChequeResponse {
    private int id;
    private String chequeNo;
    private String payeeName;
    private BigDecimal amount;
    private String amountWords;
    private String bankName;
    private Integer bankId;
    private Integer accountId;
    private LocalDate issueDate;
    private String status;
    private boolean active;
    private String lastPrinter;
    private String lastPrintResult;
    private LocalDateTime printedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String bankCode;
    private String accountHolderName;
    private String accountNumber;
    private boolean canPrint;
    private boolean canApprove;
    private boolean canReject;
    private boolean canEdit;
    private boolean canDelete;

    public ChequeResponse() {}

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
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public Integer getBankId() { return bankId; }
    public void setBankId(Integer bankId) { this.bankId = bankId; }
    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getLastPrinter() { return lastPrinter; }
    public void setLastPrinter(String lastPrinter) { this.lastPrinter = lastPrinter; }
    public String getLastPrintResult() { return lastPrintResult; }
    public void setLastPrintResult(String lastPrintResult) { this.lastPrintResult = lastPrintResult; }
    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public boolean isCanPrint() { return canPrint; }
    public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }
    public boolean isCanApprove() { return canApprove; }
    public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }
    public boolean isCanReject() { return canReject; }
    public void setCanReject(boolean canReject) { this.canReject = canReject; }
    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
