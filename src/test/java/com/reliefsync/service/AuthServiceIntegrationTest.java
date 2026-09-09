package com.reliefsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.db.Database;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.repository.UserRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthServiceIntegrationTest {

    @TempDir
    Path tempDir;

    private String databaseUrl;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        databaseUrl = "jdbc:sqlite:" + tempDir.resolve("auth.db");
        Database.init(databaseUrl);
        auth = new AuthService();
    }

    @AfterEach
    void tearDown() {
        Database.reset();
        Session.logout();
    }

    @Test
    void signupCreatesLeastPrivilegedAccountThatCanLogin() {
        User created = auth.signup("  Fatema   Akter  ", "  Fatema_01  ",
                "Secure123", "Secure123");

        assertEquals("Fatema Akter", created.fullName());
        assertEquals("fatema_01", created.username());
        assertEquals(Role.VOLUNTEER, created.role());
        String storedHash = new UserRepository().passwordHash("fatema_01").orElseThrow();
        assertNotEquals("Secure123", storedHash);
        assertEquals(created, auth.login("FATEMA_01", "Secure123").orElseThrow());
        assertTrue(auth.login("fatema_01", "wrong-password").isEmpty());
    }

    @Test
    void signedUpAccountPersistsAfterDatabaseReopens() {
        auth.signup("Persistent User", "persistent_user", "Password9", "Password9");
        Database.reset();
        Database.init(databaseUrl);

        User user = new AuthService().login("persistent_user", "Password9").orElseThrow();
        assertEquals(Role.VOLUNTEER, user.role());
    }

    @Test
    void duplicateUsernameIsRejectedCaseInsensitively() {
        auth.signup("First User", "same_user", "Password9", "Password9");
        assertThrows(IllegalStateException.class,
                () -> auth.signup("Second User", "SAME_USER", "Password9", "Password9"));
        assertEquals(1, new UserRepository().count());
    }

    @Test
    void usernameAndNameValidationRejectBadInput() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("A", "valid_user", "Password9", "Password9"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "ab", "Password9", "Password9"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "invalid-user", "Password9", "Password9"));
        assertFalse(auth.hasAnyUser());
    }

    @Test
    void passwordPolicyAndConfirmationAreEnforced() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "valid_user", "short1A", "short1A"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "valid_user", "alllowercase9", "alllowercase9"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "valid_user", "NoNumberHere", "NoNumberHere"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.signup("Valid User", "valid_user", "Password9", "Password8"));
        assertFalse(auth.hasAnyUser());
    }
}
