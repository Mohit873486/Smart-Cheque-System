package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.layout.Pane;

/**
 * @deprecated Merged into single {@link ChequeRenderEngine}. Use ChequeRenderEngine directly.
 */
@Deprecated
public final class ChequePreviewEngine {

    private ChequePreviewEngine() {
        // Utility class
    }

    public static void renderPreview(Pane targetPane, Cheque cheque, Bank bank, BankTemplateLayout layout) {
        ChequeRenderEngine.renderCheque(targetPane, cheque, bank, layout);
    }

    public static void renderLoadingState(Pane targetPane, String message) {
        ChequeRenderEngine.renderLoadingState(targetPane, message);
    }

    public static void renderEmptyState(Pane targetPane) {
        ChequeRenderEngine.renderEmptyState(targetPane);
    }

    public static void renderErrorState(Pane targetPane, String errorMessage) {
        ChequeRenderEngine.renderEmptyState(targetPane);
    }
}
