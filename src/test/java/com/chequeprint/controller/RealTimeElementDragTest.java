package com.chequeprint.controller;

import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import com.chequeprint.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RealTimeElementDragTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testElementDragUpdatesTemplateModelInRealTime() {
        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);
        AppState.getInstance().setSelectedTemplate(layout);

        // Simulate real-time element drag to new relative ratio coordinates (0.40, 0.50)
        double newXRatio = 0.40;
        double newYRatio = 0.50;
        layout.setFieldPosition(LayoutField.PAYEE, newXRatio, newYRatio);

        // Update global AppState in real-time
        AppState.getInstance().setSelectedTemplate(layout);

        BankTemplateLayout activeLayout = AppState.getInstance().getSelectedTemplate();
        assertNotNull(activeLayout, "Active template layout must not be null.");

        FieldPosition payeePos = activeLayout.get(LayoutField.PAYEE);
        assertEquals(0.40, payeePos.getXRatio(), 0.001, "Payee X ratio must match updated drag coordinate 0.40.");
        assertEquals(0.50, payeePos.getYRatio(), 0.001, "Payee Y ratio must match updated drag coordinate 0.50.");
    }
}
