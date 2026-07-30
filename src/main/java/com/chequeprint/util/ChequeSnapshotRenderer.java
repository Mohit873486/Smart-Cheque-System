package com.chequeprint.util;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import javafx.print.Printer;
import javafx.scene.image.WritableImage;

/**
 * @deprecated Relocated to {@link com.chequeprint.engine.ChequeSnapshotRenderer}. Use com.chequeprint.engine.ChequeSnapshotRenderer.
 */
@Deprecated
public class ChequeSnapshotRenderer {

    public static BankTemplateLayout resolveLayout(Bank bankTemplate) {
        return com.chequeprint.engine.ChequeSnapshotRenderer.resolveLayout(bankTemplate);
    }

    public static WritableImage renderCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        return com.chequeprint.engine.ChequeSnapshotRenderer.renderCheque(cheque, bank, layout, scale);
    }

    public static boolean printSnapshot(WritableImage snapshot, Printer printer, String jobName, BankTemplateLayout layout) {
        return com.chequeprint.engine.ChequeSnapshotRenderer.printSnapshot(snapshot, printer, jobName, layout);
    }

    public static String exportSnapshotPdf(WritableImage snapshot, String jobName, BankTemplateLayout layout) {
        return com.chequeprint.engine.ChequeSnapshotRenderer.exportSnapshotPdf(snapshot, jobName, layout);
    }
}
