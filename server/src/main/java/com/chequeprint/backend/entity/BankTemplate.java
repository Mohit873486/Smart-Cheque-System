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

    @Column(name = "bank_code", nullable = false, unique = true, length = 50)
    private String bankCode;

    @Column(name = "cheque_size", length = 50)
    private String chequeSize = "8.5x3.66in";

    @Column(nullable = false)
    private boolean micr = true;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    public BankTemplate() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getChequeSize() { return chequeSize; }
    public void setChequeSize(String chequeSize) { this.chequeSize = chequeSize; }

    public boolean isMicr() { return micr; }
    public void setMicr(boolean micr) { this.micr = micr; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
}
