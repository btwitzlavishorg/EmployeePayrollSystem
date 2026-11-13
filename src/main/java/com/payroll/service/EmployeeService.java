package com.payroll.service;

import com.payroll.dao.EmployeeDAO;
import com.payroll.dao.SalaryDAO;
import com.payroll.dao.impl.EmployeeDAOImpl;
import com.payroll.dao.impl.SalaryDAOImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.Employee;
import com.payroll.model.SalaryStructure;
import com.payroll.model.enums.EmploymentStatus;
import com.payroll.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;
    private final SalaryDAO salaryDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAOImpl();
        this.salaryDAO = new SalaryDAOImpl();
    }

    public EmployeeService(EmployeeDAO employeeDAO, SalaryDAO salaryDAO) {
        this.employeeDAO = employeeDAO;
        this.salaryDAO = salaryDAO;
    }

    public List<Employee> getAllEmployees() {
        return employeeDAO.findAll();
    }

    public Optional<Employee> getEmployeeById(int id) {
        return employeeDAO.findById(id);
    }

    public Optional<Employee> getEmployeeByCode(String code) {
        return employeeDAO.findByCode(code);
    }

    public List<Employee> searchEmployees(String keyword, Integer departmentId, EmploymentStatus status) {
        return employeeDAO.search(keyword, departmentId, status);
    }

    public int createEmployee(Employee emp, BigDecimal initialBasicSalary) {
        validateEmployee(emp);

        // Check for duplicate email
        Optional<Employee> existingEmail = employeeDAO.findByEmail(emp.getEmail());
        if (existingEmail.isPresent()) {
            throw new ValidationException("An employee with email '" + emp.getEmail() + "' already exists.");
        }

        // Generate automatic employee code if not supplied
        if (emp.getEmployeeCode() == null || emp.getEmployeeCode().trim().isEmpty()) {
            emp.setEmployeeCode(employeeDAO.generateNextEmployeeCode());
        }

        int empId = employeeDAO.save(emp);

        // Initialize base salary structure
        if (initialBasicSalary != null && initialBasicSalary.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate effective = emp.getDateOfJoining() != null ? emp.getDateOfJoining() : LocalDate.now();
            salaryDAO.saveOrUpdateStructure(empId, initialBasicSalary, effective);
        }

        return empId;
    }

    public boolean updateEmployee(Employee emp, BigDecimal updatedBasicSalary) {
        validateEmployee(emp);

        // Check for duplicate email with another employee
        Optional<Employee> existingEmail = employeeDAO.findByEmail(emp.getEmail());
        if (existingEmail.isPresent() && existingEmail.get().getEmployeeId() != emp.getEmployeeId()) {
            throw new ValidationException("Another employee with email '" + emp.getEmail() + "' already exists.");
        }

        boolean updated = employeeDAO.update(emp);

        if (updatedBasicSalary != null) {
            LocalDate effective = LocalDate.now();
            salaryDAO.saveOrUpdateStructure(emp.getEmployeeId(), updatedBasicSalary, effective);
        }

        return updated;
    }

    public boolean deleteEmployee(int employeeId) {
        return employeeDAO.delete(employeeId);
    }

    public String getNextEmployeeCode() {
        return employeeDAO.generateNextEmployeeCode();
    }

    private void validateEmployee(Employee emp) {
        ValidationUtil.validateNotEmpty(emp.getFirstName(), "First Name");
        ValidationUtil.validateNotEmpty(emp.getLastName(), "Last Name");
        ValidationUtil.validateEmail(emp.getEmail());
        ValidationUtil.validatePhone(emp.getPhone());
        ValidationUtil.validateNotEmpty(emp.getDesignation(), "Designation");
        if (emp.getDepartmentId() <= 0) {
            throw new ValidationException("Please select a valid Department.");
        }
        if (emp.getDateOfJoining() == null) {
            throw new ValidationException("Date of Joining is required.");
        }
        ValidationUtil.validateNotEmpty(emp.getBankName(), "Bank Name");
        ValidationUtil.validateNotEmpty(emp.getBankAccountNo(), "Bank Account Number");
        ValidationUtil.validateIfsc(emp.getBankIfsc());
        ValidationUtil.validatePanNumber(emp.getPanNo());
    }
}
