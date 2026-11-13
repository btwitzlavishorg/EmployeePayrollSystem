package com.payroll.service;

import com.payroll.dao.SalaryDAO;
import com.payroll.dao.impl.SalaryDAOImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.EmployeeSalaryComponent;
import com.payroll.model.SalaryComponent;
import com.payroll.model.SalaryStructure;
import com.payroll.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SalaryService {

    private final SalaryDAO salaryDAO;

    public SalaryService() {
        this.salaryDAO = new SalaryDAOImpl();
    }

    public SalaryService(SalaryDAO salaryDAO) {
        this.salaryDAO = salaryDAO;
    }

    public Optional<SalaryStructure> getStructureByEmployeeId(int employeeId) {
        return salaryDAO.getStructureByEmployeeId(employeeId);
    }

    public boolean updateEmployeeBasicSalary(int employeeId, BigDecimal basicSalary) {
        ValidationUtil.validatePositiveAmount(basicSalary, "Basic Salary");
        return salaryDAO.saveOrUpdateStructure(employeeId, basicSalary, LocalDate.now());
    }

    public List<SalaryComponent> getAllSalaryComponents() {
        return salaryDAO.getAllComponents();
    }

    public int createComponent(SalaryComponent comp) {
        ValidationUtil.validateNotEmpty(comp.getComponentCode(), "Component Code");
        ValidationUtil.validateNotEmpty(comp.getComponentName(), "Component Name");
        ValidationUtil.validatePositiveAmount(comp.getDefaultValue(), "Default Rate/Amount");
        return salaryDAO.saveComponent(comp);
    }

    public boolean updateComponent(SalaryComponent comp) {
        ValidationUtil.validateNotEmpty(comp.getComponentName(), "Component Name");
        ValidationUtil.validatePositiveAmount(comp.getDefaultValue(), "Default Rate/Amount");
        return salaryDAO.updateComponent(comp);
    }

    public List<EmployeeSalaryComponent> getEmployeeComponents(int employeeId) {
        return salaryDAO.getEmployeeComponents(employeeId);
    }

    public boolean saveEmployeeComponent(int employeeId, int componentId, BigDecimal customValue, boolean isEnabled) {
        if (customValue != null) {
            ValidationUtil.validatePositiveAmount(customValue, "Component Value");
        }
        return salaryDAO.saveEmployeeComponent(employeeId, componentId, customValue, isEnabled);
    }
}
