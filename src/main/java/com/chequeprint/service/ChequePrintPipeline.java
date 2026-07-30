package com.chequeprint.service;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * @deprecated Merged into single {@link PrintService}. Use PrintService directly.
 */
@Deprecated
public final class ChequePrintPipeline {

    private static final PrintService SERVICE = new PrintService();

    private ChequePrintPipeline() {
    }

    public static boolean execute(Cheque cheque, Bank bank, BankTemplateLayout layout, Window ownerWindow) throws Exception {
        return SERVICE.printCheque(cheque, bank, layout, ownerWindow);
    }

    public static BankTemplateLayout step1LoadTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        return SERVICE.step1LoadTemplate(cheque, bank, layout);
    }

    public static Cheque step2LoadChequeData(Cheque cheque) {
        return SERVICE.step2LoadChequeData(cheque);
    }

    public static BankTemplateLayout step3MergeDataIntoTemplate(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        return layout;
    }

    public static Node step4RenderFinalChequeLayout(Cheque cheque, Bank bank, BankTemplateLayout layout) {
        javafx.scene.layout.Pane canvas = new javafx.scene.layout.Pane();
        canvas.setPrefSize(layout.getWidthInches() * 72.0, layout.getHeightInches() * 72.0);
        com.chequeprint.util.ChequeRenderEngine.renderCheque(canvas, cheque, bank, layout);
        return canvas;
    }

    public static boolean step5SendToSelectedPrinter(Node renderedCanvas, Window ownerWindow) {
        return SERVICE.printRenderedCheque(renderedCanvas, ownerWindow);
    }

    public static void logPrePrintDiagnostics(Cheque cheque, BankTemplateLayout layout) {
        SERVICE.logPrePrintDiagnostics(cheque, layout);
    }
}
