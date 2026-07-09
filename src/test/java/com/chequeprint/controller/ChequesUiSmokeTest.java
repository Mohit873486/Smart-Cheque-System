package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.BankService;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.ChequeWorkflowService;
import com.chequeprint.service.PrintService;
import com.chequeprint.service.AuditService;
import com.chequeprint.util.SessionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChequesUiSmokeTest extends ApplicationTest {

    private ChequeController controller;
    private TableView<Cheque> chequeTable;
    private Button btnPrint;
    private Button btnApprove;

    @Override
    public void start(Stage stage) throws Exception {
        // Setup mock user session
        User mockActor = new User();
        mockActor.setUsername("admin");
        mockActor.setRole("Admin");
        SessionManager.getInstance().start(mockActor);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/cheques.fxml"));
        loader.setControllerFactory(param -> {
            ChequeController ctrl = new ChequeController();
            ctrl.chequeService = mock(ChequeService.class);
            ctrl.workflowService = mock(ChequeWorkflowService.class);
            ctrl.printService = mock(PrintService.class);
            ctrl.bankService = mock(BankService.class);
            ctrl.auditService = mock(AuditService.class);

            try {
                when(ctrl.chequeService.getAll()).thenReturn(Collections.emptyList());
                when(ctrl.bankService.getAll()).thenReturn(Collections.emptyList());
                when(ctrl.auditService.findRecent(anyInt())).thenReturn(Collections.emptyList());
            } catch (Exception e) {
                // ignore
            }
            return ctrl;
        });

        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testPrintButtonLifecycle() {
        chequeTable = lookup("#chequeTable").queryAs(TableView.class);
        btnPrint = lookup("#btnPrint").queryAs(Button.class);
        btnApprove = lookup("#btnApprove").queryAs(Button.class);

        // Create a Mock Draft Cheque
        Cheque draftCheque = new Cheque(101, "Test Payee", new BigDecimal("5000.00"), 1, LocalDate.now());
        draftCheque.setChequeNo("CHQ_DRAFT");
        draftCheque.setStatus(Cheque.Status.Draft);

        // Select the Draft Cheque in Table
        interact(() -> {
            controller.getData().setAll(draftCheque);
            chequeTable.getSelectionModel().select(draftCheque);
        });

        // 1. Verify Print is disabled for a Draft cheque
        assertTrue(btnPrint.isDisabled(), "Print button should be disabled for a Draft cheque.");

        // 2. Approve the cheque (Approve changes status to Approved)
        interact(() -> {
            draftCheque.setStatus(Cheque.Status.Approved);
            // Refresh table selection to recalculate button states
            chequeTable.getSelectionModel().clearSelection();
            chequeTable.getSelectionModel().select(draftCheque);
        });

        // 3. Verify Print becomes enabled after status becomes Approved
        assertFalse(btnPrint.isDisabled(), "Print button should be enabled after status becomes Approved.");

        // 4. Verify clicking Print does not throw
        assertDoesNotThrow(() -> {
            interact(() -> btnPrint.fire());
        });
    }
}
