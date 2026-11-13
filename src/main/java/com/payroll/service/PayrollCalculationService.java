package com.payroll.service;

import com.payroll.dao.EmployeeDAO;
import com.payroll.dao.PayrollDAO;
import com.payroll.dao.SalaryDAO;
import com.payroll.dao.impl.EmployeeDAOImpl;
import com.payroll.dao.impl.PayrollDAOImpl;
import com.payroll.dao.impl.SalaryDAOImpl;
import com.payroll.exception.DuplicateEntityException;
import com.payroll.exception.ValidationException;
import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryComponent;
import com.payroll.model.Payroll;
import com.payroll.model.PayrollDetail;
import com.payroll.model.SalaryComponent;
import com.payroll.model.SalaryStructure;
import com.payroll.model.enums.CalculationType;
import com.payroll.model.enums.ComponentType;
import com.payroll.model.enums.PayrollStatus;
import com.payroll.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PayrollCalculationService {

    private final PayrollDAO payrollDAO;
    private final EmployeeDAO employeeDAO;
    private final SalaryDAO salaryDAO;
    private final AttendanceService attendanceService;

    public PayrollCalculationService() {
        this.payrollDAO = new PayrollDAOImpl();
        this.employeeDAO = new EmployeeDAOImpl();
        this.salaryDAO = new SalaryDAOImpl();
        this.attendanceService = new AttendanceService();
    }

    public PayrollCalculationService(PayrollDAO payrollDAO, EmployeeDAO employeeDAO, SalaryDAO salaryDAO, AttendanceService attendanceService) {
        this.payrollDAO = payrollDAO;
        this.employeeDAO = employeeDAO;
        this.salaryDAO = salaryDAO;
        this.attendanceService = attendanceService;
    }

    /**
     * Calculates draft payroll for preview without persisting.
     */
    public Payroll calculateDraftPayroll(Employee employee, int month, int year, BigDecimal manualAdjustment, String adjustmentReason) {
        int workingDays = DateTimeUtil.getDaysInMonth(month, year);

        // Fetch basic salary
        Optional<SalaryStructure> structOpt = salaryDAO.getStructureByEmployeeId(employee.getEmployeeId());
        BigDecimal basicSalary = structOpt.map(SalaryStructure::getBasicSalary).orElse(BigDecimal.ZERO);

        // Attendance + unpaid leave impact (absences and half-days included)
        AttendanceService.AttendanceImpact impact =
                attendanceService.calculatePayrollImpact(employee.getEmployeeId(), month, year, workingDays);
        int unpaidDays = impact.unpaidLeaveDays;
        int presentDays = impact.presentDays;
        BigDecimal lopDays = impact.totalLopDays;

        // Calculate Pro-rated Loss of Pay (LOP) from unpaid leave, absences, and half-days
        BigDecimal lopDeduction = BigDecimal.ZERO;
        if (lopDays.compareTo(BigDecimal.ZERO) > 0 && basicSalary.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal perDayRate = basicSalary.divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
            lopDeduction = perDayRate.multiply(lopDays).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        List<PayrollDetail> details = new ArrayList<>();

        // Fetch configured components for employee
        List<EmployeeSalaryComponent> empComponents = salaryDAO.getEmployeeComponents(employee.getEmployeeId());

        // If no customized components, load active default components from catalog
        if (empComponents.isEmpty()) {
            List<SalaryComponent> globalComponents = salaryDAO.getAllComponents();
            for (SalaryComponent comp : globalComponents) {
                if (comp.isActive()) {
                    EmployeeSalaryComponent esc = new EmployeeSalaryComponent();
                    esc.setEmployeeId(employee.getEmployeeId());
                    esc.setComponentId(comp.getComponentId());
                    esc.setComponent(comp);
                    esc.setEnabled(true);
                    empComponents.add(esc);
                }
            }
        }

        for (EmployeeSalaryComponent esc : empComponents) {
            if (!esc.isEnabled()) continue;

            SalaryComponent comp = esc.getComponent();
            if (comp == null || !comp.isActive()) continue;

            BigDecimal rateOrAmount = esc.getEffectiveValue();
            BigDecimal calculatedAmount = BigDecimal.ZERO;

            if (comp.getCalculationType() == CalculationType.PERCENTAGE_OF_BASIC) {
                calculatedAmount = basicSalary.multiply(rateOrAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                calculatedAmount = rateOrAmount.setScale(2, RoundingMode.HALF_UP);
            }

            if (comp.isEarning()) {
                totalAllowances = totalAllowances.add(calculatedAmount);
                details.add(new PayrollDetail(comp.getComponentName(), ComponentType.EARNING, calculatedAmount));
            } else if (comp.isDeduction()) {
                totalDeductions = totalDeductions.add(calculatedAmount);
                details.add(new PayrollDetail(comp.getComponentName(), ComponentType.DEDUCTION, calculatedAmount));
            }
        }

        // Add LOP Deduction line item if applicable
        if (lopDeduction.compareTo(BigDecimal.ZERO) > 0) {
            totalDeductions = totalDeductions.add(lopDeduction);
            StringBuilder lopLabel = new StringBuilder("Loss of Pay (");
            lopLabel.append(lopDays.stripTrailingZeros().toPlainString()).append(" days");
            if (unpaidDays > 0) {
                lopLabel.append(": unpaid leave ").append(unpaidDays);
            }
            if (impact.absentDays > 0) {
                lopLabel.append(unpaidDays > 0 ? ", " : ": ").append("absent ").append(impact.absentDays);
            }
            if (impact.halfDays > 0) {
                lopLabel.append((unpaidDays > 0 || impact.absentDays > 0) ? ", " : ": ")
                        .append("half-day ").append(impact.halfDays);
            }
            lopLabel.append(")");
            details.add(new PayrollDetail(lopLabel.toString(), ComponentType.DEDUCTION, lopDeduction));
        }

        // Add Manual Adjustment if provided
        if (manualAdjustment != null && manualAdjustment.compareTo(BigDecimal.ZERO) != 0) {
            String reasonText = (adjustmentReason != null && !adjustmentReason.trim().isEmpty()) 
                    ? "Manual Adjustment: " + adjustmentReason.trim() 
                    : "Manual Salary Adjustment";
            if (manualAdjustment.compareTo(BigDecimal.ZERO) > 0) {
                totalAllowances = totalAllowances.add(manualAdjustment);
                details.add(new PayrollDetail(reasonText, ComponentType.EARNING, manualAdjustment));
            } else {
                BigDecimal positiveDed = manualAdjustment.abs();
                totalDeductions = totalDeductions.add(positiveDed);
                details.add(new PayrollDetail(reasonText, ComponentType.DEDUCTION, positiveDed));
            }
        }

        BigDecimal grossSalary = basicSalary.add(totalAllowances).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        Payroll payroll = new Payroll();
        payroll.setEmployeeId(employee.getEmployeeId());
        payroll.setEmployeeCode(employee.getEmployeeCode());
        payroll.setEmployeeName(employee.getFullName());
        payroll.setDepartmentName(employee.getDepartmentName());
        payroll.setDesignation(employee.getDesignation());
        payroll.setBankName(employee.getBankName());
        payroll.setBankAccountNo(employee.getBankAccountNo());
        payroll.setPanNo(employee.getPanNo());

        payroll.setPayrollMonth(month);
        payroll.setPayrollYear(year);
        payroll.setBasicSalary(basicSalary);
        payroll.setTotalAllowances(totalAllowances);
        payroll.setGrossSalary(grossSalary);
        payroll.setTotalDeductions(totalDeductions);
        payroll.setNetSalary(netSalary);

        payroll.setWorkingDays(workingDays);
        payroll.setPresentDays(presentDays);
        payroll.setUnpaidLeaveDays(lopDays.setScale(0, RoundingMode.HALF_UP).intValue());
        // Generated payrolls await payment (dashboard counts PROCESSED as unpaid)
        payroll.setStatus(PayrollStatus.PROCESSED);
        payroll.setDetails(details);

        return payroll;
    }

    /**
     * Generates and saves monthly payroll for an individual employee with duplicate check.
     */
    public int processEmployeePayroll(int employeeId, int month, int year, BigDecimal manualAdjustment, String adjustmentReason) {
        // Enforce duplicate check
        Optional<Payroll> existing = payrollDAO.findByEmployeeAndPeriod(employeeId, month, year);
        if (existing.isPresent()) {
            throw new DuplicateEntityException("Payroll for this employee for " + 
                    DateTimeUtil.getMonthName(month) + " " + year + " has already been generated (Status: " + 
                    existing.get().getStatus().getDisplayName() + ").");
        }

        Employee emp = employeeDAO.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Employee with ID " + employeeId + " not found."));

        Payroll p = calculateDraftPayroll(emp, month, year, manualAdjustment, adjustmentReason);
        return payrollDAO.savePayroll(p);
    }

    /**
     * Generates monthly payroll in batch for all active employees.
     */
    public List<Payroll> processMonthlyBatchPayroll(int month, int year) {
        List<Employee> allEmployees = employeeDAO.findAll();
        List<Payroll> processed = new ArrayList<>();

        for (Employee emp : allEmployees) {
            if (emp.isActive()) {
                Optional<Payroll> existing = payrollDAO.findByEmployeeAndPeriod(emp.getEmployeeId(), month, year);
                if (!existing.isPresent()) {
                    Payroll p = calculateDraftPayroll(emp, month, year, BigDecimal.ZERO, null);
                    payrollDAO.savePayroll(p);
                    processed.add(p);
                }
            }
        }
        return processed;
    }

    public boolean markAsPaid(int payrollId, LocalDate paymentDate, String paymentMethod, String reference) {
        return payrollDAO.updateStatus(payrollId, PayrollStatus.PAID, paymentDate, paymentMethod, reference);
    }

    public List<Payroll> getPayrollsByPeriod(int month, int year) {
        return payrollDAO.findByPeriod(month, year);
    }

    public Optional<Payroll> getPayrollById(int payrollId) {
        return payrollDAO.findById(payrollId);
    }

    public boolean deletePayroll(int payrollId) {
        return payrollDAO.delete(payrollId);
    }
}
