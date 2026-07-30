package com.chequeprint.preview;

import com.chequeprint.engine.ChequeRenderEngine;

/**
 * @deprecated Relocated to unified {@link ChequeRenderEngine}. Use com.chequeprint.engine.ChequeRenderEngine.
 */
@Deprecated
public class ChequeRenderer {

    public static void renderPreview(javafx.scene.layout.Pane targetPane, com.chequeprint.model.Cheque cheque, com.chequeprint.model.Bank bank, com.chequeprint.model.BankTemplateLayout layout) {
        ChequeRenderEngine.renderCheque(targetPane, cheque, bank, layout);
    }

    public void render(java.awt.Graphics2D g2, ChequeData data, BankTemplate template, boolean includeGuideBoxes) {
        // Legacy AWT render compatibility delegate
    }
}
