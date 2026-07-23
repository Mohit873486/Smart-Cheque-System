package com.chequeprint.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "template_field")
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "x_position", nullable = false)
    private Double xPosition;

    @Column(name = "y_position", nullable = false)
    private Double yPosition;

    @Column(name = "font_size", nullable = false)
    private Integer fontSize = 12;

    @Column(name = "font_family", nullable = false)
    private String fontFamily = "Arial";

    public TemplateField() {
    }

    public TemplateField(Long id, Long templateId, String fieldName, Double xPosition, Double yPosition, Integer fontSize, String fontFamily) {
        this.id = id;
        this.templateId = templateId;
        this.fieldName = fieldName;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.fontSize = fontSize != null ? fontSize : 12;
        this.fontFamily = fontFamily != null ? fontFamily : "Arial";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Double getxPosition() {
        return xPosition;
    }

    public void setxPosition(Double xPosition) {
        this.xPosition = xPosition;
    }

    public Double getyPosition() {
        return yPosition;
    }

    public void setyPosition(Double yPosition) {
        this.yPosition = yPosition;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }
}
