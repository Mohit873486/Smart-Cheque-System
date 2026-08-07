package com.chequeprint.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "cheques")
public class Cheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cheque_no", unique = true, length = 50)
    private String chequeNo;

    @NotBlank(message = "Payee name is required")
    @Size(max = 150, message = "Max 150 chars")
    @Pattern(regexp = "^[a-zA-Z0-9 .'-]+$", message = "Invalid characters in payee name")
    @Column(name = "payee_name", nullable = false, length = 150)
    private String payeeName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_words", length = 250)
    private String amountWords;

    @Column(name = "bank_id", nullable = true)
    private Integer bankId;

    @Column(name = "account_id", nullable = true)
    private Integer accountId;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "last_printer")
    private String lastPrinter;

    @Column(name = "last_print_result")
    private String lastPrintResult;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_id", insertable = false, updatable = false)
    private BankTemplate bankTemplate;

    public enum Status {
        Draft, Pending, Deposited, Cleared, Bounced, Approved, Rejected, Printed, Cancelled
    }

    public Cheque() {}

    // ── Getters / Setters ───────────────────────────────────────────

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

    public Integer getBankId() { return bankId; }
    public void setBankId(Integer bankId) { this.bankId = bankId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

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

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BankTemplate getBankTemplate() { return bankTemplate; }
    public void setBankTemplate(BankTemplate bankTemplate) { this.bankTemplate = bankTemplate; }

    public String getBankName() {
        return bankTemplate != null ? bankTemplate.getBankName() : null;
    }
}
