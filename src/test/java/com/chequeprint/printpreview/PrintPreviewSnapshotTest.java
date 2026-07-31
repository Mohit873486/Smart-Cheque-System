package com.chequeprint.printpreview;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PrintPreviewSnapshotTest {

    @Test
    public void testHtmlTemplateGenerationForPrintPreview() {
        PrintHtmlTemplateService htmlService = new PrintHtmlTemplateService();
        
        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        Cheque cheque = new Cheque();
        cheque.setPayeeName("Sharma Traders");
        cheque.setAmount(new BigDecimal("12500.50"));
        cheque.setIssueDate(LocalDate.now());

        BankTemplateLayout layout = new BankTemplateLayout(8.0, 3.66);

        String html = htmlService.buildChequeHtml(cheque, bank, layout);

        assertNotNull(html, "HTML template output must not be null.");
        assertTrue(html.contains("Sharma Traders"), "HTML output must contain payee name Sharma Traders.");
        assertTrue(html.contains("12,500.50"), "HTML output must contain formatted amount 12,500.50.");
    }
}
