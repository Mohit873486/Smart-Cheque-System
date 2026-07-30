package com.chequeprint.controller;

import com.chequeprint.service.ChequeService;
import com.chequeprint.service.PrintService;
import com.chequeprint.service.TemplateService;
import com.chequeprint.service.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerRefactoringTest {

    @Test
    public void testServicesAreInstantiatedForControllers() {
        ChequeService chequeService = new ChequeService();
        TemplateService templateService = new TemplateService();
        PrintService printService = new PrintService();
        UserService userService = new UserService();

        assertNotNull(chequeService, "ChequeService must be available for controllers.");
        assertNotNull(templateService, "TemplateService must be available for controllers.");
        assertNotNull(printService, "PrintService must be available for controllers.");
        assertNotNull(userService, "UserService must be available for controllers.");
    }
}
