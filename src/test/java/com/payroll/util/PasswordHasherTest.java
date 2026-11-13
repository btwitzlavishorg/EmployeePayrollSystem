package com.payroll.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordHasherTest {

    @Test
    public void hashAndVerifySucceedsForCorrectPassword() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("admin123", salt);
        assertTrue(PasswordHasher.verify("admin123", hash, salt));
    }

    @Test
    public void verifyFailsForWrongPassword() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("admin123", salt);
        assertFalse(PasswordHasher.verify("wrong", hash, salt));
    }

    @Test
    public void differentSaltsProduceDifferentHashes() {
        String h1 = PasswordHasher.hashPassword("secret", PasswordHasher.generateSalt());
        String h2 = PasswordHasher.hashPassword("secret", PasswordHasher.generateSalt());
        assertNotEquals(h1, h2);
    }
}
