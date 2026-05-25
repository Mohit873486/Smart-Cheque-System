package com.chequeprint.preview;

import java.awt.Color;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class BankTemplate implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String key;
  private final String displayName;
  private final int accentColorRgb;
  private final EnumMap<ChequeField, FieldPosition> positions;

  public BankTemplate(String key, String displayName, Color accentColor, Map<ChequeField, FieldPosition> positions) {
    this.key = key;
    this.displayName = displayName;
    this.accentColorRgb = accentColor.getRGB();
    this.positions = new EnumMap<>(ChequeField.class);
    for (ChequeField field : ChequeField.values()) {
      FieldPosition pos = positions.get(field);
      this.positions.put(field, pos != null ? new FieldPosition(pos) : new FieldPosition());
    }
  }

  public String getKey() {
    return key;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Color getAccentColor() {
    return new Color(accentColorRgb, true);
  }

  public FieldPosition getPosition(ChequeField field) {
    FieldPosition pos = positions.get(field);
    return pos != null ? pos : new FieldPosition();
  }

  public void setPosition(ChequeField field, int x, int y) {
    positions.computeIfAbsent(field, f -> new FieldPosition()).set(x, y);
  }

  public void adjustPosition(ChequeField field, int deltaX, int deltaY) {
    positions.computeIfAbsent(field, f -> new FieldPosition()).adjust(deltaX, deltaY);
  }

  public Map<ChequeField, FieldPosition> copyPositions() {
    EnumMap<ChequeField, FieldPosition> copy = new EnumMap<>(ChequeField.class);
    for (Map.Entry<ChequeField, FieldPosition> entry : positions.entrySet()) {
      copy.put(entry.getKey(), new FieldPosition(entry.getValue()));
    }
    return copy;
  }

  public BankTemplate copy() {
    return new BankTemplate(key, displayName, getAccentColor(), copyPositions());
  }

  @Override
  public String toString() {
    return displayName;
  }
}
