package com.payroll.service;

import com.payroll.dao.UserDAO;
import com.payroll.dao.impl.UserDAOImpl;
import com.payroll.exception.AuthenticationException;
import com.payroll.model.User;
import com.payroll.util.PasswordHasher;

import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Username and password are required.");
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());
        if (!userOpt.isPresent()) {
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new AuthenticationException("This account is inactive or suspended. Please contact IT support.");
        }

        boolean valid = PasswordHasher.verify(password, user.getPasswordHash(), user.getSalt());
        if (!valid) {
            throw new AuthenticationException("Invalid username or password.");
        }

        return user;
    }

    /**
     * Authenticates credentials and creates a server-side session token.
     */
    public String login(String username, String password) {
        User user = authenticate(username, password);
        return SessionManager.createSession(user);
    }

    public void logout() {
        SessionManager.invalidateCurrentSession();
    }
}
