package com.chequeprint.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cheques")
public class Cheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cheque_no", unique = true, length = 50)
    private String chequeNo;

    @Column(name = "payee_name", nullable = false, length = 150)
    private String payeeName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_words", length = 250)
    private String amountWords;

    @Column(name = "bank_id", nullable = false)
    private int bankId;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

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

    public Cheque(String chequeNo, String payeeName, BigDecimal amount, String amountWords, int bankId, LocalDate issueDate, Status status) {
        this.chequeNo = chequeNo;
        this.payeeName = payeeName;
        this.amount = amount;
        this.amountWords = amountWords;
        this.bankId = bankId;
        this.issueDate = issueDate;
        this.status = status;
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

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getBankName() {
        return bankTemplate != null ? bankTemplate.getBankName() : null;
    }
}
