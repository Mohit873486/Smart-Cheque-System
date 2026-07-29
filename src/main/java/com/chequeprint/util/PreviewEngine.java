package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.layout.Pane;

/**
 * Single Unified Preview Engine for rendering cheque layouts onto JavaFX Panes.
 * Accepts template coordinates and cheque data, computing X and Y field positions
 * dynamically from the template with ZERO hardcoded positions.
 */
public final class PreviewEngine {

    private PreviewEngine() {
        // Utility class
    }

    /**
     * Renders cheque using template layout coordinates and cheque data onto a target Pane.
     *
     * @param targetPane JavaFX container pane to render preview inside
     * @param cheque     Active cheque data
     * @param layout     Template layout containing field coordinates
     */
    public static void render(Pane targetPane, Cheque cheque, BankTemplateLayout layout) {
        render(targetPane, cheque, AppState.getInstance().getSelectedBank(), layout);
    }

    /**
     * Renders cheque using template layout coordinates, cheque data, and bank details.
     *
     * @param targetPane JavaFX container pane to render preview inside
     * @param cheque     Active cheque data
     * @param bank       Active bank details
     * @param layout     Template layout containing field coordinates
     */
    public static void render(Pane targetPane, Cheque cheque, Bank bank, BankTemplateLayout layout) {
        if (layout == null) {
            layout = AppState.getInstance().getSelectedTemplate();
        }
        if (bank == null) {
            bank = AppState.getInstance().getSelectedBank();
        }
        if (cheque == null) {
            cheque = AppState.getInstance().getCurrentCheque();
        }
        ChequePreviewEngine.renderPreview(targetPane, cheque, bank, layout);
    }
}
