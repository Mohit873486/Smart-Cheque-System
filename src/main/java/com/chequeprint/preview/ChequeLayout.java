package com.chequeprint.preview;

public final class ChequeLayout {
  public static final double WIDTH_INCH = 8.50;
  public static final double HEIGHT_INCH = 3.66;
  public static final int LOGICAL_DPI = 300;
  public static final int CANVAS_WIDTH = (int) Math.round(WIDTH_INCH * LOGICAL_DPI);
  public static final int CANVAS_HEIGHT = (int) Math.round(HEIGHT_INCH * LOGICAL_DPI);

  private ChequeLayout() {
  }
}
