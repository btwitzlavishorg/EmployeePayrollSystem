package com.payroll.service;

import com.payroll.dao.DepartmentDAO;
import com.payroll.dao.impl.DepartmentDAOImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.Department;
import com.payroll.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

public class DepartmentService {

    private final DepartmentDAO departmentDAO;

    public DepartmentService() {
        this.departmentDAO = new DepartmentDAOImpl();
    }

    public DepartmentService(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    public List<Department> getAllDepartments() {
        return departmentDAO.findAll();
    }

    public Optional<Department> getDepartmentById(int id) {
        return departmentDAO.findById(id);
    }

    public int createDepartment(Department dept) {
        validateDepartment(dept);
        Optional<Department> existing = departmentDAO.findByCode(dept.getDepartmentCode());
        if (existing.isPresent()) {
            throw new ValidationException("Department with code '" + dept.getDepartmentCode() + "' already exists.");
        }
        return departmentDAO.save(dept);
    }

    public boolean updateDepartment(Department dept) {
        validateDepartment(dept);
        Optional<Department> existing = departmentDAO.findByCode(dept.getDepartmentCode());
        if (existing.isPresent() && existing.get().getDepartmentId() != dept.getDepartmentId()) {
            throw new ValidationException("Another department with code '" + dept.getDepartmentCode() + "' already exists.");
        }
        return departmentDAO.update(dept);
    }

    public boolean deleteDepartment(int departmentId) {
        int empCount = departmentDAO.getEmployeeCount(departmentId);
        if (empCount > 0) {
            throw new ValidationException("Cannot delete department: " + empCount + " employee(s) are currently assigned to this department. Please reassign them first.");
        }
        return departmentDAO.delete(departmentId);
    }

    private void validateDepartment(Department dept) {
        ValidationUtil.validateNotEmpty(dept.getDepartmentCode(), "Department Code");
        ValidationUtil.validateNotEmpty(dept.getDepartmentName(), "Department Name");
    }
}
