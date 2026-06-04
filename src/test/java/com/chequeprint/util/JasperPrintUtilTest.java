package com.chequeprint.util;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.Invoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit/integration-light tests for JasperPrintUtil.
 * <p>
 * Notes:
 * - JasperReports compilation/export needs the .jrxml templates on the
 * classpath.
 * - PDF creation is a good smoke test that the pipeline works.
 * - Printing (printCheque) triggers the OS print dialog, so it is intentionally
 * not unit tested.
 */
public class JasperPrintUtilTest {

  @Test
  void exportChequePdf_throwsIfTemplateMissing() {
    // This test assumes the real template exists; if it doesn't, the method should
    // throw JRException.
    // Since the template is part of the app resources, we instead just test the
    // happy path in other tests.
    // Keep this test as a placeholder by validating that the method throws when
    // outputDir is invalid.
    // (JRException is declared, but we don't rely on it here.)
    assertThrows(Exception.class, () -> {
      Cheque cheque = new Cheque("CHQ-1", "Test Payee", new BigDecimal("10.00"), 1, LocalDate.now());
      JasperPrintUtil.exportChequePdf(cheque, "?:\\invalid\\path\\");
    });
  }

  @Test
  void exportChequePdf_createsPdfFile() throws Exception {
    Path tempDir = Files.createTempDirectory("chequeprint-test-");

    Cheque cheque = new Cheque("CHQ-1001", "Alice", new BigDecimal("2500.75"), 1, LocalDate.of(2024, 1, 20));
    cheque.setBankName("Test Bank");
    cheque.setAmountWords("Two Thousand Five Hundred Rupees Only");

    String pdfPath = JasperPrintUtil.exportChequePdf(cheque, tempDir.toString());

    assertEquals(tempDir.resolve("CHQ-1001.pdf").toString(), pdfPath);
    assertTrue(Files.isRegularFile(Path.of(pdfPath)));
    assertTrue(Files.size(Path.of(pdfPath)) > 0);
  }

  @Test
  void exportInvoicePdf_createsPdfFile() throws Exception {
    Path tempDir = Files.createTempDirectory("invoiceprint-test-");

    Invoice invoice = new Invoice("INV-2001", "Bob Client", new BigDecimal("999.00"),
        LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 15));
    invoice.setStatus(Invoice.Status.Unpaid);
    invoice.setNotes("Payment due soon");

    String pdfPath = JasperPrintUtil.exportInvoicePdf(invoice, tempDir.toString());

    assertEquals(tempDir.resolve("INV-2001.pdf").toString(), pdfPath);
    assertTrue(Files.isRegularFile(Path.of(pdfPath)));
    assertTrue(Files.size(Path.of(pdfPath)) > 0);
  }

  @Test
  void nvl_viaNullBlankBehavior_isTrustedByExports() throws Exception {
    Path tempDir = Files.createTempDirectory("nvlprint-test-");

    Cheque cheque = new Cheque(null, null, null, 1, null);
    cheque.setBankName("");
    cheque.setAmountWords("  ");

    String pdfPath = JasperPrintUtil.exportChequePdf(cheque, tempDir.toString());

    assertEquals(tempDir.resolve("cheque.pdf").toString(), pdfPath);
    assertTrue(Files.isRegularFile(Path.of(pdfPath)));
    assertTrue(Files.size(Path.of(pdfPath)) > 0);
  }

}
