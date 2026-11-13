package com.payroll.dao;

import com.payroll.model.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentDAO {
    List<Department> findAll();
    Optional<Department> findById(int departmentId);
    Optional<Department> findByCode(String code);
    int save(Department department);
    boolean update(Department department);
    boolean delete(int departmentId);
    int getEmployeeCount(int departmentId);
}
