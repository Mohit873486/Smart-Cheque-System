package com.chequeprint.engine;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.Node;
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

    @Test
    public void testInitializePreviewElementsCreatesAndStoresFieldNodes() {
        Pane previewPane = new Pane();

        ChequeRenderEngine.initializePreviewElements(previewPane);

        assertFalse(previewPane.getChildren().isEmpty(),
                "Bank/template load should create preview field nodes up front.");
        assertTrue(previewPane.getProperties().containsKey("LABEL_CACHE"),
                "Preview field references must be stored for later updates.");
    }

    @Test
    public void testChequeRenderEngineReusesCachedLabelsAcrossRenders() {
        Pane previewPane = new Pane();
        previewPane.setPrefSize(576, 263);

        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Global Tech Solutions");
        cheque.setAmount(new BigDecimal("100000.00"));
        cheque.setIssueDate(LocalDate.now());
        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);

        ChequeRenderEngine.renderCheque(previewPane, cheque, bank, layout);
        Node firstPayeeNode = previewPane.getChildren().get(1);

        cheque.setPayeeName("Sharma Traders");
        ChequeRenderEngine.renderCheque(previewPane, cheque, bank, layout);

        assertSame(firstPayeeNode, previewPane.getChildren().get(1),
                "Preview render must update cached labels instead of recreating JavaFX nodes.");
    }

    @Test
    public void testChequeRenderEngineReattachesCachedLabelsAfterPaneWasCleared() {
        Pane previewPane = new Pane();
        previewPane.setPrefSize(576, 263);

        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Global Tech Solutions");
        cheque.setAmount(new BigDecimal("100000.00"));
        cheque.setIssueDate(LocalDate.now());
        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);

        ChequeRenderEngine.renderCheque(previewPane, cheque, bank, layout);
        Node firstPayeeNode = previewPane.getChildren().get(1);

        previewPane.getChildren().clear();
        cheque.setPayeeName("Sharma Traders");
        ChequeRenderEngine.renderCheque(previewPane, cheque, bank, layout);

        assertSame(firstPayeeNode, previewPane.getChildren().get(1),
                "Cached labels should be reattached after an external pane clear.");
    }
}
