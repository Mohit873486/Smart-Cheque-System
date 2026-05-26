package com.chequeprint.model;

import java.io.Serializable;

public class FieldPosition implements Serializable {
    private static final long serialVersionUID = 1L;

    private double xRatio;
    private double yRatio;
    private double widthRatio;
    private double heightRatio;

    public FieldPosition() {
        this(0.5, 0.5);
    }

    public FieldPosition(double xRatio, double yRatio) {
        this(xRatio, yRatio, 0.0, 0.0);
    }

    public FieldPosition(double xRatio, double yRatio, double widthRatio, double heightRatio) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    public double getXRatio() {
        return xRatio;
    }

    public void setXRatio(double xRatio) {
        this.xRatio = xRatio;
    }

    public double getYRatio() {
        return yRatio;
    }

    public void setYRatio(double yRatio) {
        this.yRatio = yRatio;
    }

    public double getWidthRatio() {
        return widthRatio;
    }

    public void setWidthRatio(double widthRatio) {
        this.widthRatio = widthRatio;
    }

    public double getHeightRatio() {
        return heightRatio;
    }

    public void setHeightRatio(double heightRatio) {
        this.heightRatio = heightRatio;
    }

    public FieldPosition copy() {
        return new FieldPosition(xRatio, yRatio, widthRatio, heightRatio);
    }
}
