package com.payroll.service;

import com.payroll.model.User;
import com.payroll.model.enums.UserRole;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class SessionManagerTest {

    @After
    public void cleanup() {
        SessionManager.clearRequest();
    }

    @Test
    public void createBindAndInvalidateSession() {
        User user = new User();
        user.setUserId(10);
        user.setUsername("tester");
        user.setRole(UserRole.ADMIN);
        user.setStatus("ACTIVE");

        String token = SessionManager.createSession(user);
        assertNotNull(token);
        assertTrue(SessionManager.bind(token));
        assertTrue(SessionManager.isLoggedIn());
        assertEquals("tester", SessionManager.getCurrentUser().getUsername());
        assertTrue(SessionManager.isAdmin());
        assertTrue(SessionManager.canManageHr());

        SessionManager.invalidateCurrentSession();
        assertFalse(SessionManager.isLoggedIn());
        assertFalse(SessionManager.bind(token));
    }

    @Test
    public void hrManagerCanManageHrButIsNotAdmin() {
        User user = new User();
        user.setUserId(2);
        user.setUsername("hrmanager");
        user.setRole(UserRole.HR_MANAGER);
        user.setStatus("ACTIVE");

        String token = SessionManager.createSession(user);
        SessionManager.bind(token);
        assertFalse(SessionManager.isAdmin());
        assertTrue(SessionManager.canManageHr());
        assertTrue(SessionManager.canManagePayroll());
        SessionManager.invalidateCurrentSession();
    }
}
