package com.chequeprint.ui;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class UiDesignSystemTest {

    @Test
    public void testStylesheetResourceExists() {
        URL cssUrl = getClass().getResource("/css/style.css");
        assertNotNull(cssUrl, "Main stylesheet resource style.css must exist.");
    }
}
