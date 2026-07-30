package com.chequeprint.state;

import com.chequeprint.model.Bank;
import com.chequeprint.model.BankTemplateLayout;
import com.chequeprint.model.User;
import javafx.print.Printer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DedicatedAppStateTest {

    @BeforeEach
    public void setUp() {
        AppState.getInstance().clear();
    }

    @Test
    public void testAppStateSingletonInstanceIsUnique() {
        AppState instance1 = AppState.getInstance();
        AppState instance2 = AppState.getInstance();

        assertNotNull(instance1, "AppState instance must not be null.");
        assertSame(instance1, instance2, "AppState.getInstance() must return the identical singleton instance.");
    }

    @Test
    public void testAppStateStoresAndRetrievesLoggedInUser() {
        User user = new User();
        user.setUsername("admin");
        user.setName("System Administrator");

        AppState.getInstance().setLoggedInUser(user);

        User current = AppState.getInstance().getLoggedInUser();
        assertNotNull(current, "Logged in user must not be null.");
        assertEquals("admin", current.getUsername());
    }

    @Test
    public void testAppStateStoresSelectedBankAndTemplate() {
        Bank bank = new Bank("State Bank of India", "SBI", "DEFAULT", true);
        BankTemplateLayout layout = new BankTemplateLayout();

        AppState.getInstance().setSelectedBank(bank);
        AppState.getInstance().setSelectedTemplate(layout);

        assertEquals(bank, AppState.getInstance().getSelectedBank());
        assertEquals(layout, AppState.getInstance().getSelectedTemplate());
    }

    @Test
    public void testClearResetsState() {
        User user = new User();
        user.setUsername("admin");
        AppState.getInstance().setLoggedInUser(user);

        AppState.getInstance().clear();

        assertNull(AppState.getInstance().getLoggedInUser(), "LoggedInUser must be null after calling clear().");
        assertNull(AppState.getInstance().getSelectedBank(), "SelectedBank must be null after calling clear().");
        assertNull(AppState.getInstance().getSelectedTemplate(), "SelectedTemplate must be null after calling clear().");
    }
}
