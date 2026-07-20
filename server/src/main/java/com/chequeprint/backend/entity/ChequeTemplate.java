package com.chequeprint.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cheque_templates")
public class ChequeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false)
    private Long bankId;

    @Column(name = "name_x")
    private Double nameX;

    @Column(name = "name_y")
    private Double nameY;

    @Column(name = "date_x")
    private Double dateX;

    @Column(name = "date_y")
    private Double dateY;

    @Column(name = "amount_x")
    private Double amountX;

    @Column(name = "amount_y")
    private Double amountY;

    @Column(name = "signature_x")
    private Double signatureX;

    @Column(name = "signature_y")
    private Double signatureY;

    public ChequeTemplate() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public Double getNameX() {
        return nameX;
    }

    public void setNameX(Double nameX) {
        this.nameX = nameX;
    }

    public Double getNameY() {
        return nameY;
    }

    public void setNameY(Double nameY) {
        this.nameY = nameY;
    }

    public Double getDateX() {
        return dateX;
    }

    public void setDateX(Double dateX) {
        this.dateX = dateX;
    }

    public Double getDateY() {
        return dateY;
    }

    public void setDateY(Double dateY) {
        this.dateY = dateY;
    }

    public Double getAmountX() {
        return amountX;
    }

    public void setAmountX(Double amountX) {
        this.amountX = amountX;
    }

    public Double getAmountY() {
        return amountY;
    }

    public void setAmountY(Double amountY) {
        this.amountY = amountY;
    }

    public Double getSignatureX() {
        return signatureX;
    }

    public void setSignatureX(Double signatureX) {
        this.signatureX = signatureX;
    }

    public Double getSignatureY() {
        return signatureY;
    }

    public void setSignatureY(Double signatureY) {
        this.signatureY = signatureY;
    }
}
