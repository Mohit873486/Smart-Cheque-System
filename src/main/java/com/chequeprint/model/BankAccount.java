package com.chequeprint.model;

import java.math.BigDecimal;

public class BankAccount {
    private int id;
    private String accountNumber;
    private String accountHolderName;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private BigDecimal balance;

    public BankAccount() {}

    public BankAccount(String accountNumber, String accountHolderName, String bankName, String branchName, String ifscCode, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.bankName = bankName;
        this.branchName = branchName;
        this.ifscCode = ifscCode;
        this.balance = balance;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Override
    public String toString() {
        return bankName + " - " + accountNumber + " (" + accountHolderName + ")";
    }
}
