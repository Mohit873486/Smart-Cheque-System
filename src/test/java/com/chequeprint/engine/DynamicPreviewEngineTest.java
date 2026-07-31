package com.chequeprint.engine;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.FieldPosition;
import com.chequeprint.model.LayoutField;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicPreviewEngineTest {

    @Test
    public void testChequeRenderEnginePositionsNodesDynamicallyFromLayout() {
        Pane canvas = new Pane();
        canvas.setPrefSize(800, 400);

        Bank bank = new Bank("HDFC Bank", "HDFC", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Sharma Traders");
        cheque.setAmount(new BigDecimal("12500.50"));

        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);
        // Custom dynamic position for Payee
        layout.setFieldPosition(LayoutField.PAYEE, 0.25, 0.35);

        ChequeRenderEngine.renderCheque(canvas, cheque, bank, layout);

        assertFalse(canvas.getChildren().isEmpty(), "Canvas must contain rendered field labels.");
        
        // Find payee label node
        Label payeeLabel = null;
        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof Label lbl && "Sharma Traders".equals(lbl.getText())) {
                payeeLabel = lbl;
                break;
            }
        }

        assertNotNull(payeeLabel, "Payee label must be rendered with cheque payee text.");
        assertEquals(0.25 * 800.0, payeeLabel.getLayoutX(), 0.01, "Payee layout X position must match 0.25 * width ratio.");
        assertEquals(0.35 * 400.0, payeeLabel.getLayoutY(), 0.01, "Payee layout Y position must match 0.35 * height ratio.");
    }
}
