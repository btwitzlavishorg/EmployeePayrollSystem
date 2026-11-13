package com.payroll.dao;

import com.payroll.model.DashboardStats;
import com.payroll.model.Payroll;
import com.payroll.model.enums.PayrollStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollDAO {
    int savePayroll(Payroll payroll);
    boolean updatePayroll(Payroll payroll);
    boolean updateStatus(int payrollId, PayrollStatus status, LocalDate paymentDate, String paymentMethod, String ref);
    boolean delete(int payrollId);
    Optional<Payroll> findById(int payrollId);
    Optional<Payroll> findByEmployeeAndPeriod(int employeeId, int month, int year);
    List<Payroll> findByPeriod(int month, int year);
    List<Payroll> getRecentPayrolls(int limit);
    DashboardStats getDashboardStats();
    List<Payroll> getAnnualPayrolls(int year);
}
