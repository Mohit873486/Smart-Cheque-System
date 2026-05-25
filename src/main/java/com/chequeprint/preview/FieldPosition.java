package com.chequeprint.preview;

import java.io.Serializable;

public class FieldPosition implements Serializable {
  private static final long serialVersionUID = 1L;

  private int x;
  private int y;

  public FieldPosition() {
    this(0, 0);
  }

  public FieldPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public FieldPosition(FieldPosition other) {
    this(other.x, other.y);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public void set(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void adjust(int deltaX, int deltaY) {
    this.x += deltaX;
    this.y += deltaY;
  }
}
