package com.chequeprint.model;

/**
 * Shared layout model representing the dynamic coordinate positions, dimensions, and size specs of a cheque template.
 */
public class ChequeLayout extends BankTemplateLayout {
    private static final long serialVersionUID = 1L;

    public ChequeLayout() {
        super();
    }

    public ChequeLayout(double widthInches, double heightInches) {
        super(widthInches, heightInches);
    }
}
