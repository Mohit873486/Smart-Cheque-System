package com.chequeprint.engine;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.Cheque;
import com.chequeprint.service.FxPrinterService;
import com.chequeprint.util.BankTemplateLayoutStore;
import com.chequeprint.util.ChequeSizeCodec;
import javafx.print.Printer;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

import java.io.File;

/**
 * Image Snapshot Renderer delegating to {@link ChequeRenderEngine}.
 */
public class ChequeSnapshotRenderer {

    public static BankTemplateLayout resolveLayout(Bank bankTemplate) {
        BankTemplateLayoutStore layoutStore = new BankTemplateLayoutStore();
        BankTemplateLayout fromSize = ChequeSizeCodec.decodeLayout(bankTemplate != null ? bankTemplate.getChequeSize() : null);
        if (bankTemplate == null || bankTemplate.getBankCode() == null || bankTemplate.getBankCode().isBlank()) {
            fromSize.ensureAllFields();
            return fromSize;
        }

        BankTemplateLayout stored = layoutStore.loadAll().get(bankTemplate.getBankCode());
        if (stored != null) {
            stored.ensureAllFields();
            return stored;
        }

        fromSize.ensureAllFields();
        return fromSize;
    }

    public static WritableImage renderCheque(Cheque cheque, Bank bank, BankTemplateLayout layout, double scale) {
        return ChequeRenderEngine.renderChequeToImage(cheque, bank, layout, scale);
    }

    public static boolean printSnapshot(WritableImage snapshot, Printer printer, String jobName, BankTemplateLayout layout) {
        if (snapshot == null) return false;
        ImageView imageView = new ImageView(snapshot);
        double w = layout != null ? layout.getWidthInches() * 72.0 : 576.0;
        double h = layout != null ? layout.getHeightInches() * 72.0 : 263.0;
        Pane pane = new Pane(imageView);
        pane.setPrefSize(w, h);
        return FxPrinterService.printNode(pane, null);
    }

    public static String exportSnapshotPdf(WritableImage snapshot, String jobName, BankTemplateLayout layout) {
        if (snapshot == null) return null;
        try {
            File tempFile = File.createTempFile(jobName != null ? jobName : "Cheque", ".pdf");
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
