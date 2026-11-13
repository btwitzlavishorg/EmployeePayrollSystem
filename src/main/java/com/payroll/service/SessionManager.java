package com.payroll.service;

import com.payroll.model.User;
import com.payroll.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie/token-based session store with per-request ThreadLocal binding.
 * Supports concurrent HTTP clients without sharing a single JVM-wide user.
 */
public class SessionManager {

    private static final long SESSION_TTL_HOURS = 12;
    private static final Map<String, SessionEntry> SESSIONS = new ConcurrentHashMap<>();
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    private static class SessionEntry {
        final User user;
        final LocalDateTime createdAt;
        volatile LocalDateTime lastAccess;

        SessionEntry(User user) {
            this.user = user;
            this.createdAt = LocalDateTime.now();
            this.lastAccess = this.createdAt;
        }

        boolean isExpired() {
            return lastAccess.plusHours(SESSION_TTL_HOURS).isBefore(LocalDateTime.now());
        }
    }

    public static String createSession(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        SESSIONS.put(token, new SessionEntry(user));
        return token;
    }

    public static boolean bind(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        SessionEntry entry = SESSIONS.get(token);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                SESSIONS.remove(token);
            }
            return false;
        }
        entry.lastAccess = LocalDateTime.now();
        CURRENT_USER.set(entry.user);
        CURRENT_TOKEN.set(token);
        return true;
    }

    public static void clearRequest() {
        CURRENT_USER.remove();
        CURRENT_TOKEN.remove();
    }

    public static synchronized User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static synchronized boolean isLoggedIn() {
        return CURRENT_USER.get() != null;
    }

    public static synchronized boolean isAdmin() {
        User user = CURRENT_USER.get();
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    public static synchronized boolean canManageHr() {
        User user = CURRENT_USER.get();
        if (user == null) return false;
        UserRole role = user.getRole();
        return role == UserRole.ADMIN || role == UserRole.HR_MANAGER;
    }

    public static synchronized boolean canManagePayroll() {
        User user = CURRENT_USER.get();
        if (user == null) return false;
        UserRole role = user.getRole();
        return role == UserRole.ADMIN || role == UserRole.HR_MANAGER || role == UserRole.ACCOUNTANT;
    }

    public static void invalidateCurrentSession() {
        String token = CURRENT_TOKEN.get();
        if (token != null) {
            SESSIONS.remove(token);
        }
        clearRequest();
    }

    public static void invalidate(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    /** @deprecated Use createSession + bind for HTTP sessions. Kept for compatibility. */
    @Deprecated
    public static synchronized void setCurrentUser(User user) {
        if (user == null) {
            clearRequest();
        } else {
            CURRENT_USER.set(user);
        }
    }

    /** @deprecated Use invalidateCurrentSession. */
    @Deprecated
    public static synchronized void clearSession() {
        invalidateCurrentSession();
    }

    public static LocalDateTime getLoginTime() {
        String token = CURRENT_TOKEN.get();
        if (token == null) return null;
        SessionEntry entry = SESSIONS.get(token);
        return entry != null ? entry.createdAt : null;
    }
}
