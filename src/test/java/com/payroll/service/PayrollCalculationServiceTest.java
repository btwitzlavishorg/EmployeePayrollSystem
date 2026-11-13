package com.payroll.service;

import com.payroll.dao.AttendanceDAO;
import com.payroll.dao.EmployeeDAO;
import com.payroll.dao.PayrollDAO;
import com.payroll.dao.SalaryDAO;
import com.payroll.model.*;
import com.payroll.model.enums.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

public class PayrollCalculationServiceTest {

    @Test
    public void draftPayrollAppliesLopAndSetsProcessedStatus() {
        Employee emp = new Employee();
        emp.setEmployeeId(1);
        emp.setEmployeeCode("EMP001");
        emp.setFirstName("Ada");
        emp.setLastName("Lovelace");
        emp.setDepartmentName("Engineering");
        emp.setDesignation("Engineer");

        SalaryStructure structure = new SalaryStructure();
        structure.setEmployeeId(1);
        structure.setBasicSalary(new BigDecimal("31000.00"));

        StubSalaryDAO salaryDAO = new StubSalaryDAO(structure);
        StubAttendanceDAO attendanceDAO = new StubAttendanceDAO();
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.of(2024, 1, 1));
        leave.setEndDate(LocalDate.of(2024, 1, 1));
        leave.setLeaveType(LeaveType.UNPAID);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setTotalDays(1);
        attendanceDAO.unpaidLeaves.add(leave);

        PayrollCalculationService service = new PayrollCalculationService(
                new StubPayrollDAO(),
                new StubEmployeeDAO(emp),
                salaryDAO,
                new AttendanceService(attendanceDAO)
        );

        Payroll payroll = service.calculateDraftPayroll(emp, 1, 2024, BigDecimal.ZERO, null);

        assertEquals(PayrollStatus.PROCESSED, payroll.getStatus());
        assertEquals(31, payroll.getWorkingDays());
        assertEquals(1, payroll.getUnpaidLeaveDays());
        assertEquals(0, new BigDecimal("30000.00").compareTo(payroll.getNetSalary()));
        assertTrue(payroll.getDetails().stream()
                .anyMatch(d -> d.getComponentName().startsWith("Loss of Pay")));
    }

    static class StubPayrollDAO implements PayrollDAO {
        @Override public int savePayroll(Payroll payroll) { return 1; }
        @Override public boolean updatePayroll(Payroll payroll) { return true; }
        @Override public Optional<Payroll> findById(int payrollId) { return Optional.empty(); }
        @Override public Optional<Payroll> findByEmployeeAndPeriod(int employeeId, int month, int year) { return Optional.empty(); }
        @Override public List<Payroll> findByPeriod(int month, int year) { return Collections.emptyList(); }
        @Override public boolean updateStatus(int payrollId, PayrollStatus status, LocalDate paymentDate, String paymentMethod, String reference) { return true; }
        @Override public boolean delete(int payrollId) { return true; }
        @Override public DashboardStats getDashboardStats() { return new DashboardStats(); }
        @Override public List<Payroll> getRecentPayrolls(int limit) { return Collections.emptyList(); }
        @Override public List<Payroll> getAnnualPayrolls(int year) { return Collections.emptyList(); }
    }

    static class StubEmployeeDAO implements EmployeeDAO {
        private final Employee emp;
        StubEmployeeDAO(Employee emp) { this.emp = emp; }
        @Override public int save(Employee employee) { return 1; }
        @Override public boolean update(Employee employee) { return true; }
        @Override public boolean delete(int employeeId) { return true; }
        @Override public Optional<Employee> findById(int employeeId) { return Optional.of(emp); }
        @Override public Optional<Employee> findByCode(String code) { return Optional.of(emp); }
        @Override public Optional<Employee> findByEmail(String email) { return Optional.of(emp); }
        @Override public List<Employee> findAll() { return Collections.singletonList(emp); }
        @Override public List<Employee> search(String keyword, Integer departmentId, EmploymentStatus status) { return Collections.singletonList(emp); }
        @Override public String generateNextEmployeeCode() { return "EMP002"; }
    }

    static class StubSalaryDAO implements SalaryDAO {
        private final SalaryStructure structure;
        StubSalaryDAO(SalaryStructure structure) { this.structure = structure; }
        @Override public Optional<SalaryStructure> getStructureByEmployeeId(int employeeId) { return Optional.of(structure); }
        @Override public boolean saveOrUpdateStructure(int employeeId, BigDecimal basicSalary, LocalDate effectiveDate) { return true; }
        @Override public List<SalaryComponent> getAllComponents() { return Collections.emptyList(); }
        @Override public Optional<SalaryComponent> getComponentById(int componentId) { return Optional.empty(); }
        @Override public Optional<SalaryComponent> getComponentByCode(String code) { return Optional.empty(); }
        @Override public int saveComponent(SalaryComponent component) { return 1; }
        @Override public boolean updateComponent(SalaryComponent component) { return true; }
        @Override public List<EmployeeSalaryComponent> getEmployeeComponents(int employeeId) { return Collections.emptyList(); }
        @Override public boolean saveEmployeeComponent(int employeeId, int componentId, BigDecimal customValue, boolean isEnabled) { return true; }
        @Override public boolean deleteEmployeeComponents(int employeeId) { return true; }
    }

    static class StubAttendanceDAO implements AttendanceDAO {
        List<LeaveRequest> unpaidLeaves = new ArrayList<>();
        @Override public boolean markAttendance(Attendance attendance) { return true; }
        @Override public List<Attendance> getAttendanceByDate(LocalDate date) { return Collections.emptyList(); }
        @Override public List<Attendance> getAttendanceByEmployeeAndMonth(int employeeId, int month, int year) { return Collections.emptyList(); }
        @Override public int saveLeaveRequest(LeaveRequest request) { return 1; }
        @Override public List<LeaveRequest> getAllLeaveRequests() { return Collections.emptyList(); }
        @Override public List<LeaveRequest> getLeaveRequestsByEmployee(int employeeId) { return Collections.emptyList(); }
        @Override public boolean updateLeaveStatus(int leaveId, LeaveStatus status, int reviewedBy, String reviewComments) { return true; }
        @Override public int countApprovedUnpaidLeaveDays(int employeeId, int month, int year) {
            return unpaidLeaves.stream().mapToInt(LeaveRequest::getTotalDays).sum();
        }
        @Override public int countPresentDays(int employeeId, int month, int year) { return 0; }
        @Override public int countDaysByStatus(int employeeId, int month, int year, String status) { return 0; }
        @Override public List<LeaveRequest> findApprovedUnpaidLeavesOverlapping(int employeeId, LocalDate monthStart, LocalDate monthEnd) { return unpaidLeaves; }
    }
}
