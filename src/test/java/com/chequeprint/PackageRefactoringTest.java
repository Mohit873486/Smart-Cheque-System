package com.chequeprint;

import com.chequeprint.engine.ChequeRenderEngine;
import com.chequeprint.state.AppState;
import com.chequeprint.util.NumberToWordsConverter;
import com.chequeprint.util.PrinterUtils;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PackageRefactoringTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testAppStateRelocatedToStatePackage() {
        assertNotNull(AppState.getInstance(), "com.chequeprint.state.AppState singleton must not be null.");
    }

    @Test
    public void testChequeRenderEngineRelocatedToEnginePackage() {
        Pane canvas = new Pane();
        canvas.setPrefSize(576, 263);

        ChequeRenderEngine.renderEmptyState(canvas);
        assertFalse(canvas.getChildren().isEmpty(), "com.chequeprint.engine.ChequeRenderEngine must render placeholder node.");
    }

    @Test
    public void testUtilPackageContainsPureUtilitiesOnly() {
        String words = NumberToWordsConverter.convert(1500.0);
        assertNotNull(words);
        assertTrue(words.contains("One Thousand"));
    }
}
