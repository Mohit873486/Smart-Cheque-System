package com.chequeprint.preview;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BankTemplateStore {
  private final Path storagePath;

  public BankTemplateStore() {
    Path appDir = Paths.get(System.getProperty("user.home"), ".chequeprint");
    this.storagePath = appDir.resolve("bank-templates.bin");
  }

  public Map<String, BankTemplate> loadTemplates() {
    Map<String, BankTemplate> templates = defaultTemplates();

    if (!Files.exists(storagePath)) {
      return templates;
    }

    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(storagePath))) {
      Object obj = in.readObject();
      if (obj instanceof Map<?, ?> saved) {
        for (Object key : saved.keySet()) {
          Object value = saved.get(key);
          if (key instanceof String templateKey && value instanceof BankTemplate template) {
            templates.put(templateKey, template.copy());
          }
        }
      }
    } catch (Exception ignored) {
      // Keep defaults if persisted templates are unavailable or invalid.
    }

    return templates;
  }

  public void saveTemplates(Map<String, BankTemplate> templates) throws IOException {
    Files.createDirectories(storagePath.getParent());
    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(storagePath))) {
      out.writeObject(new LinkedHashMap<>(templates));
    }
  }

  public static Map<String, BankTemplate> defaultTemplates() {
    Map<String, BankTemplate> map = new LinkedHashMap<>();

    map.put("SBI", new BankTemplate("SBI", "SBI", new Color(0x0D2B6B), positions(
        260, 430,
        2060, 360,
        1930, 525,
        320, 615)));

    map.put("AXIS", new BankTemplate("AXIS", "Axis Bank", new Color(0x8A1547), positions(
        275, 438,
        2050, 348,
        1900, 538,
        340, 626)));

    map.put("HDFC", new BankTemplate("HDFC", "HDFC Bank", new Color(0x004C8F), positions(
        250, 446,
        2035, 358,
        1888, 547,
        315, 636)));

    map.put("ICICI", new BankTemplate("ICICI", "ICICI Bank", new Color(0xA65A1F), positions(
        285, 432,
        2068, 352,
        1945, 529,
        355, 620)));

    return map;
  }

  private static Map<ChequeField, FieldPosition> positions(
      int payeeX, int payeeY,
      int dateX, int dateY,
      int amountNumX, int amountNumY,
      int amountWordsX, int amountWordsY) {
    EnumMap<ChequeField, FieldPosition> positions = new EnumMap<>(ChequeField.class);
    positions.put(ChequeField.PAYEE_NAME, new FieldPosition(payeeX, payeeY));
    positions.put(ChequeField.DATE, new FieldPosition(dateX, dateY));
    positions.put(ChequeField.AMOUNT_NUMBER, new FieldPosition(amountNumX, amountNumY));
    positions.put(ChequeField.AMOUNT_WORDS, new FieldPosition(amountWordsX, amountWordsY));
    return positions;
  }
}
