package com.payroll.dao;

import com.payroll.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findByUsername(String username);
    Optional<User> findById(int userId);
    List<User> findAll();
    int save(User user);
    boolean update(User user);
    boolean updatePassword(int userId, String passwordHash, String salt);
}
