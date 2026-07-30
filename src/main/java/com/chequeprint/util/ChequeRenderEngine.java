package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

/**
 * @deprecated Relocated to {@link com.chequeprint.engine.ChequeRenderEngine}. Use com.chequeprint.engine.ChequeRenderEngine.
 */
@Deprecated
public final class ChequeRenderEngine {

    private ChequeRenderEngine() {
    }

    public static void renderCheque(Pane targetPane, Cheque cheque, Bank bank, BankTemplateLayout layout) {
        com.chequeprint.engine.ChequeRenderEngine.renderCheque(targetPane, cheque, bank, layout);
    }

    public static WritableImage renderChequeToImage(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        return com.chequeprint.engine.ChequeRenderEngine.renderChequeToImage(cheque, bank, layout, scale);
    }

    public static void renderLoadingState(Pane targetPane, String message) {
        com.chequeprint.engine.ChequeRenderEngine.renderLoadingState(targetPane, message);
    }

    public static void renderEmptyState(Pane targetPane) {
        com.chequeprint.engine.ChequeRenderEngine.renderEmptyState(targetPane);
    }
}
