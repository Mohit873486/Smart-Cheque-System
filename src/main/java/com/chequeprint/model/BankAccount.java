package com.chequeprint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankAccount {
    private Integer id;
    private String accountNumber;
    private String accountHolderName;
    private String bankName;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("ifsc")
    private String ifsc;

    private BigDecimal balance;
    private String signaturePath;

    public BankAccount() {}

    public BankAccount(String accountNumber, String accountHolderName, String bankName, String branch, String ifsc, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.bankName = bankName;
        this.branch = branch;
        this.ifsc = ifsc;
        this.balance = balance;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getBranchName() { return branch != null ? branch : ""; }
    public void setBranchName(String branchName) { this.branch = branchName; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getIfscCode() { return ifsc != null ? ifsc : ""; }
    public void setIfscCode(String ifscCode) { this.ifsc = ifscCode; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }

    @Override
    public String toString() {
        return bankName + " - " + accountNumber + " (" + (accountHolderName != null ? accountHolderName : bankName) + ")";
    }
}
