package com.payroll.dao;

import com.payroll.model.Employee;
import com.payroll.model.enums.EmploymentStatus;
import java.util.List;
import java.util.Optional;

public interface EmployeeDAO {
    List<Employee> findAll();
    Optional<Employee> findById(int employeeId);
    Optional<Employee> findByCode(String employeeCode);
    Optional<Employee> findByEmail(String email);
    List<Employee> search(String keyword, Integer departmentId, EmploymentStatus status);
    int save(Employee employee);
    boolean update(Employee employee);
    boolean delete(int employeeId);
    String generateNextEmployeeCode();
}
