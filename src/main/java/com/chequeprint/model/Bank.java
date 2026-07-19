package com.chequeprint.model;

public class Bank {
    private Integer id;
    private String bankName;
    private String bankCode;
    private String chequeSize;
    private boolean micr;
    private String logoPath;

    public Bank() {}

    public Bank(String bankName, String bankCode, String chequeSize, boolean micr) {
        this.bankName   = bankName;
        this.bankCode   = bankCode;
        this.chequeSize = chequeSize;
        this.micr       = micr;
    }

    public Integer getId()                          { return id; }
    public void setId(Integer id)                   { this.id = id; }
    public String getBankName()                 { return bankName; }
    public void setBankName(String n)           { this.bankName = n; }
    public String getBankCode()                 { return bankCode; }
    public void setBankCode(String c)           { this.bankCode = c; }
    public String getChequeSize()               { return chequeSize; }
    public void setChequeSize(String s)         { this.chequeSize = s; }
    public boolean isMicr()                     { return micr; }
    public void setMicr(boolean m)              { this.micr = m; }
    public String getLogoPath()                 { return logoPath; }
    public void setLogoPath(String p)           { this.logoPath = p; }

    @Override public String toString()          { return bankName + " (" + bankCode + ")"; }
}