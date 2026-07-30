package com.chequeprint.engine;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SinglePreviewSystemTest {

    @Test
    public void testChequeRenderEngineIsSingleSourceOfTruthForPreview() {
        Pane previewPane = new Pane();
        previewPane.setPrefSize(576, 263);

        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Global Tech Solutions");
        cheque.setAmount(new BigDecimal("100000.00"));
        cheque.setIssueDate(LocalDate.now());

        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);

        ChequeRenderEngine.renderCheque(previewPane, cheque, bank, layout);

        assertFalse(previewPane.getChildren().isEmpty(), "Single preview engine must populate nodes in previewPane.");
        assertEquals("LABEL_CACHE", previewPane.getProperties().keySet().iterator().next().toString());
    }
}
