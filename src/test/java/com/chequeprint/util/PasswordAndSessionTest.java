package com.chequeprint.util;

import com.chequeprint.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordAndSessionTest {

    @Test
    public void testPasswordHashing() {
        String plainPassword = "mySuperSecretPassword123";
        
        // Hash it
        String hash = PasswordUtil.hash(plainPassword);
        assertNotNull(hash);
        assertTrue(PasswordUtil.isBcryptHash(hash));
        
        // Matches
        assertTrue(PasswordUtil.matches(plainPassword, hash));
        assertFalse(PasswordUtil.matches("wrongpassword", hash));
    }

    @Test
    public void testSessionHandling() {
        User user = new User();
        user.setId(999);
        user.setUsername("testuser");
        user.setName("Test User");
        
        // Start session
        SessionManager.start(user);
        assertTrue(SessionManager.currentUser().isPresent());
        assertEquals("testuser", SessionManager.currentUser().get().getUsername());
        assertFalse(SessionManager.isExpired());
        
        // Update activity
        SessionManager.updateActivity();
        assertFalse(SessionManager.isExpired());
        
        // Clear session
        SessionManager.clear();
        assertFalse(SessionManager.currentUser().isPresent());
    }
}
