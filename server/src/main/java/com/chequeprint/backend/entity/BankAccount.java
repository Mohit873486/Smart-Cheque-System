package com.chequeprint.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bank name is required")
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Column(name = "ifsc", nullable = false)
    private String ifsc;

    @Column(name = "branch")
    private String branch;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public BankAccount() {
    }

    public BankAccount(Long id, String bankName, String accountNumber, String ifsc, String branch) {
        this.id = id;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifsc = ifsc;
        this.branch = branch;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfsc() {
        return ifsc;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
