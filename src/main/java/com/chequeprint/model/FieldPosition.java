package com.chequeprint.model;

import java.io.Serializable;

public class FieldPosition implements Serializable {
    private static final long serialVersionUID = 1L;

    private double xRatio;
    private double yRatio;

    public FieldPosition() {
        this(0.5, 0.5);
    }

    public FieldPosition(double xRatio, double yRatio) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;
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

    public FieldPosition copy() {
        return new FieldPosition(xRatio, yRatio);
    }
}
