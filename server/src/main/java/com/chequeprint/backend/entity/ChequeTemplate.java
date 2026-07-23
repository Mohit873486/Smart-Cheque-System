package com.chequeprint.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cheque_template")
public class ChequeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false)
    private Long bankId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "width", nullable = false)
    private Double width = 203.20;

    @Column(name = "height", nullable = false)
    private Double height = 92.00;

    public ChequeTemplate() {
    }

    public ChequeTemplate(Long id, Long bankId, String templateName, Double width, Double height) {
        this.id = id;
        this.bankId = bankId;
        this.templateName = templateName;
        this.width = width != null ? width : 203.20;
        this.height = height != null ? height : 92.00;
    }

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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }
}
