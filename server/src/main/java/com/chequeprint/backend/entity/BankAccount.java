package com.chequeprint.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bank name is mandatory")
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @NotBlank(message = "Account number is mandatory")
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @NotBlank(message = "IFSC code is mandatory")
    @Column(name = "ifsc", nullable = false)
    private String ifsc;

    @NotBlank(message = "Branch is mandatory")
    @Column(name = "branch", nullable = false)
    private String branch;

    @Column(name = "signature_path")
    private String signaturePath;

    // Constructors
    public BankAccount() {}

    public BankAccount(String bankName, String accountNumber, String ifsc, String branch, String signaturePath) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifsc = ifsc;
        this.branch = branch;
        this.signaturePath = signaturePath;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }
}
