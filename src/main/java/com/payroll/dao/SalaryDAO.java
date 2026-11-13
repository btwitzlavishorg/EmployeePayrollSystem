package com.payroll.dao;

import com.payroll.model.EmployeeSalaryComponent;
import com.payroll.model.SalaryComponent;
import com.payroll.model.SalaryStructure;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryDAO {
    Optional<SalaryStructure> getStructureByEmployeeId(int employeeId);
    boolean saveOrUpdateStructure(int employeeId, BigDecimal basicSalary, LocalDate effectiveDate);
    List<SalaryComponent> getAllComponents();
    Optional<SalaryComponent> getComponentById(int componentId);
    Optional<SalaryComponent> getComponentByCode(String code);
    int saveComponent(SalaryComponent component);
    boolean updateComponent(SalaryComponent component);
    List<EmployeeSalaryComponent> getEmployeeComponents(int employeeId);
    boolean saveEmployeeComponent(int employeeId, int componentId, BigDecimal customValue, boolean isEnabled);
    boolean deleteEmployeeComponents(int employeeId);
}
