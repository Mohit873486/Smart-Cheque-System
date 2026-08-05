package com.chequeprint.backend.entity;

import jakarta.persistence.*;

/**
 * Normalized layout field for a cheque template.
 * Replaces the old config_json LONGTEXT column in cheque_template.
 *
 * Each row represents one LayoutField (BANK_LOGO, DATE, PAYEE, AMOUNT_NUMBER,
 * AMOUNT_WORDS, SIGNATURE, MICR) for a specific ChequeTemplate, storing its
 * position as normalized ratios relative to the canvas size.
 */
@Entity
@Table(
    name = "template_layout_fields",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tlf_template_field",
        columnNames = {"template_id", "field_name"}
    )
)
public class TemplateLayoutField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /** Matches the LayoutField enum name (e.g. "BANK_LOGO", "DATE", "PAYEE"). */
    @Column(name = "field_name", nullable = false, length = 30)
    private String fieldName;

    /** Horizontal position as a ratio of canvas width [0.0 – 1.0]. */
    @Column(name = "x_ratio", nullable = false)
    private Double xRatio = 0.0;

    /** Vertical position as a ratio of canvas height [0.0 – 1.0]. */
    @Column(name = "y_ratio", nullable = false)
    private Double yRatio = 0.0;

    /** Field width as a ratio of canvas width [0.0 – 1.0]. */
    @Column(name = "width_ratio", nullable = false)
    private Double widthRatio = 0.0;

    /** Field height as a ratio of canvas height [0.0 – 1.0]. */
    @Column(name = "height_ratio", nullable = false)
    private Double heightRatio = 0.0;

    public TemplateLayoutField() {}

    public TemplateLayoutField(Long templateId, String fieldName,
                               double xRatio, double yRatio,
                               double widthRatio, double heightRatio) {
        this.templateId  = templateId;
        this.fieldName   = fieldName;
        this.xRatio      = xRatio;
        this.yRatio      = yRatio;
        this.widthRatio  = widthRatio;
        this.heightRatio = heightRatio;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public Double getXRatio() { return xRatio; }
    public void setXRatio(Double xRatio) { this.xRatio = xRatio; }

    public Double getYRatio() { return yRatio; }
    public void setYRatio(Double yRatio) { this.yRatio = yRatio; }

    public Double getWidthRatio() { return widthRatio; }
    public void setWidthRatio(Double widthRatio) { this.widthRatio = widthRatio; }

    public Double getHeightRatio() { return heightRatio; }
    public void setHeightRatio(Double heightRatio) { this.heightRatio = heightRatio; }
}
