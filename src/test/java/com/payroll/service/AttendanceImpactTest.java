package com.payroll.service;

import com.payroll.dao.AttendanceDAO;
import com.payroll.model.Attendance;
import com.payroll.model.LeaveRequest;
import com.payroll.model.enums.AttendanceStatus;
import com.payroll.model.enums.LeaveStatus;
import com.payroll.model.enums.LeaveType;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AttendanceImpactTest {

    @Test
    public void unpaidLeaveAndAbsenceIncreaseLop() {
        StubAttendanceDAO dao = new StubAttendanceDAO();

        LeaveRequest leave = new LeaveRequest();
        leave.setLeaveType(LeaveType.UNPAID);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setStartDate(LocalDate.of(2024, 1, 2));
        leave.setEndDate(LocalDate.of(2024, 1, 3));
        leave.setTotalDays(2);
        dao.unpaidLeaves.add(leave);

        Attendance absent = new Attendance();
        absent.setAttendanceDate(LocalDate.of(2024, 1, 5));
        absent.setStatus(AttendanceStatus.ABSENT);
        dao.attendance.add(absent);

        Attendance half = new Attendance();
        half.setAttendanceDate(LocalDate.of(2024, 1, 6));
        half.setStatus(AttendanceStatus.HALF_DAY);
        dao.attendance.add(half);

        AttendanceService service = new AttendanceService(dao);
        AttendanceService.AttendanceImpact impact = service.calculatePayrollImpact(1, 1, 2024, 31);

        assertEquals(2, impact.unpaidLeaveDays);
        assertEquals(1, impact.absentDays);
        assertEquals(1, impact.halfDays);
        assertEquals(0, new BigDecimal("3.5").compareTo(impact.totalLopDays));
        assertEquals(28, impact.presentDays); // 31 - 3.5 = 27.5 → rounds HALF_UP to 28
    }

    @Test
    public void noAttendanceUsesOnlyUnpaidLeave() {
        StubAttendanceDAO dao = new StubAttendanceDAO();
        LeaveRequest leave = new LeaveRequest();
        leave.setLeaveType(LeaveType.UNPAID);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setStartDate(LocalDate.of(2024, 3, 10));
        leave.setEndDate(LocalDate.of(2024, 3, 12));
        leave.setTotalDays(3);
        dao.unpaidLeaves.add(leave);

        AttendanceService service = new AttendanceService(dao);
        AttendanceService.AttendanceImpact impact = service.calculatePayrollImpact(1, 3, 2024, 31);

        assertEquals(3, impact.unpaidLeaveDays);
        assertEquals(0, impact.absentDays);
        assertEquals(3, impact.totalLopDays.intValue());
        assertEquals(28, impact.presentDays);
        assertFalse(impact.hasAttendanceRecords);
    }

    static class StubAttendanceDAO implements AttendanceDAO {
        List<LeaveRequest> unpaidLeaves = new ArrayList<>();
        List<Attendance> attendance = new ArrayList<>();

        @Override public boolean markAttendance(Attendance attendance) { return true; }
        @Override public List<Attendance> getAttendanceByDate(LocalDate date) { return Collections.emptyList(); }
        @Override public List<Attendance> getAttendanceByEmployeeAndMonth(int employeeId, int month, int year) { return attendance; }
        @Override public int saveLeaveRequest(LeaveRequest request) { return 1; }
        @Override public List<LeaveRequest> getAllLeaveRequests() { return Collections.emptyList(); }
        @Override public List<LeaveRequest> getLeaveRequestsByEmployee(int employeeId) { return Collections.emptyList(); }
        @Override public boolean updateLeaveStatus(int leaveId, com.payroll.model.enums.LeaveStatus status, int reviewedBy, String reviewComments) { return true; }
        @Override
        public int countApprovedUnpaidLeaveDays(int employeeId, int month, int year) {
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            int days = 0;
            for (LeaveRequest leave : unpaidLeaves) {
                LocalDate start = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
                LocalDate end = leave.getEndDate().isAfter(monthEnd) ? monthEnd : leave.getEndDate();
                if (!end.isBefore(start)) {
                    days += (int) (end.toEpochDay() - start.toEpochDay() + 1);
                }
            }
            return days;
        }
        @Override public int countPresentDays(int employeeId, int month, int year) { return 0; }
        @Override public int countDaysByStatus(int employeeId, int month, int year, String status) { return 0; }
        @Override public List<LeaveRequest> findApprovedUnpaidLeavesOverlapping(int employeeId, LocalDate monthStart, LocalDate monthEnd) { return unpaidLeaves; }
    }
}
