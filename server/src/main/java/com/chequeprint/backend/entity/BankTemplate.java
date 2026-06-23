package com.chequeprint.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_templates")
public class BankTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    public BankTemplate() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
}
