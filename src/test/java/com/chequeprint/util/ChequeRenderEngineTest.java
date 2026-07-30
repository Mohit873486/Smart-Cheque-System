package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ChequeRenderEngineTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testRenderChequePopulatesPaneChildren() {
        Pane canvas = new Pane();
        canvas.setPrefSize(576, 263);

        Cheque cheque = new Cheque(1, "Acme Corp", new BigDecimal("15000.00"), 1, LocalDate.now());
        Bank bank = new Bank();
        bank.setBankCode("SBI");
        bank.setBankName("State Bank of India");
        BankTemplateLayout layout = new BankTemplateLayout();

        ChequeRenderEngine.renderCheque(canvas, cheque, bank, layout);

        assertFalse(canvas.getChildren().isEmpty(), "Canvas should contain rendered label nodes for template fields.");
    }

    @Test
    public void testRenderEmptyStateRendersPlaceholder() {
        Pane canvas = new Pane();
        canvas.setPrefSize(576, 263);

        ChequeRenderEngine.renderEmptyState(canvas);

        assertFalse(canvas.getChildren().isEmpty(), "Canvas should render placeholder nodes when template is null.");
    }
}
