module com.chequeprint {

  // ── JavaFX ──
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.base;

  // ── Database ──
  requires java.sql;

  // ── JasperReports and MySQL are automatic JPMS modules on the module path ──
  requires jasperreports;
  requires mysql.connector.j;

  // ── Open packages so FXML loader (reflection) can access controllers ──
  opens com.chequeprint to javafx.fxml, javafx.graphics;
  opens com.chequeprint.controller to javafx.fxml;
  opens com.chequeprint.model to javafx.base, javafx.fxml, jasperreports;
  opens com.chequeprint.config to javafx.fxml;
  opens com.chequeprint.dao to javafx.fxml;
  opens com.chequeprint.service to javafx.fxml;
  opens com.chequeprint.util to javafx.fxml;

  // ── Exports ──
  exports com.chequeprint;
  exports com.chequeprint.controller;
  exports com.chequeprint.model;
  exports com.chequeprint.service;
  exports com.chequeprint.dao;
  exports com.chequeprint.util;
  exports com.chequeprint.config;
}