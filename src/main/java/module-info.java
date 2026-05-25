module com.chequeprint {

    // ── JavaFX ──────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // ── Database ─────────────────────────────────────────────────────
    requires java.sql;
    requires java.desktop;

    // ── Third-party (automatic modules on the module path) ───────────
    requires jasperreports;
    requires mysql.connector.j;

    // ── Open packages for FXML reflection ────────────────────────────
    // FIX: javafx.graphics added to the 'opens' for com.chequeprint so the
    //      Application subclass (MainApp) is accessible via reflection when
    //      JavaFX launches it. Without this, some JVM configurations throw
    //      InaccessibleObjectException at startup.
    opens com.chequeprint            to javafx.fxml, javafx.graphics;
    opens com.chequeprint.controller to javafx.fxml;

    // model must be open to javafx.base so ObservableList/PropertyValueFactory
    // can introspect field getters via reflection at runtime.
    opens com.chequeprint.model      to javafx.base, javafx.fxml, jasperreports;

    opens com.chequeprint.config     to javafx.fxml;
    opens com.chequeprint.dao        to javafx.fxml;
    opens com.chequeprint.service    to javafx.fxml;
    opens com.chequeprint.util       to javafx.fxml;

    // ── Exports ──────────────────────────────────────────────────────
    exports com.chequeprint;
    exports com.chequeprint.controller;
    exports com.chequeprint.model;
    exports com.chequeprint.service;
    exports com.chequeprint.dao;
    exports com.chequeprint.util;
    exports com.chequeprint.config;
}
