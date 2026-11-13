package com.payroll.service;

import com.payroll.dao.AttendanceDAO;
import com.payroll.dao.impl.AttendanceDAOImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.Attendance;
import com.payroll.model.LeaveRequest;
import com.payroll.model.enums.AttendanceStatus;
import com.payroll.model.enums.LeaveStatus;
import com.payroll.model.enums.LeaveType;
import com.payroll.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttendanceService {

    private final AttendanceDAO attendanceDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAOImpl();
    }

    public AttendanceService(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    public boolean markAttendance(int employeeId, LocalDate date, AttendanceStatus status,
                                  LocalTime checkIn, LocalTime checkOut, String notes) {
        Attendance att = new Attendance(employeeId, date, status, checkIn, checkOut, notes);
        return attendanceDAO.markAttendance(att);
    }

    public List<Attendance> getAttendanceForDate(LocalDate date) {
        return attendanceDAO.getAttendanceByDate(date);
    }

    public List<Attendance> getEmployeeAttendance(int employeeId, int month, int year) {
        return attendanceDAO.getAttendanceByEmployeeAndMonth(employeeId, month, year);
    }

    public int applyLeave(int employeeId, LeaveType leaveType, LocalDate start, LocalDate end, String reason) {
        if (start == null || end == null) {
            throw new ValidationException("Start date and End date are required.");
        }
        if (end.isBefore(start)) {
            throw new ValidationException("End date cannot be earlier than start date.");
        }
        ValidationUtil.validateNotEmpty(reason, "Leave Reason");

        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
        LeaveRequest req = new LeaveRequest();
        req.setEmployeeId(employeeId);
        req.setLeaveType(leaveType);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setTotalDays(totalDays);
        req.setReason(reason);
        req.setStatus(LeaveStatus.PENDING);

        return attendanceDAO.saveLeaveRequest(req);
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return attendanceDAO.getAllLeaveRequests();
    }

    public boolean reviewLeave(int leaveId, LeaveStatus status, int reviewerUserId, String comments) {
        if (status == LeaveStatus.PENDING) {
            throw new ValidationException("Please approve or reject the leave request.");
        }
        return attendanceDAO.updateLeaveStatus(leaveId, status, reviewerUserId, comments);
    }

    public int getApprovedUnpaidLeaveDays(int employeeId, int month, int year) {
        return attendanceDAO.countApprovedUnpaidLeaveDays(employeeId, month, year);
    }

    public int getPresentDays(int employeeId, int month, int year) {
        return attendanceDAO.countPresentDays(employeeId, month, year);
    }

    /**
     * Builds payroll attendance impact for a month:
     * - Unpaid leave days overlapping the month
     * - ABSENT days not already covered by unpaid leave
     * - HALF_DAY counts as 0.5 LOP day
     * When no daily attendance exists, only unpaid leave applies (no blanket absence).
     */
    public AttendanceImpact calculatePayrollImpact(int employeeId, int month, int year, int workingDays) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        int unpaidLeaveDays = attendanceDAO.countApprovedUnpaidLeaveDays(employeeId, month, year);
        Set<LocalDate> unpaidDates = new HashSet<>();
        List<LeaveRequest> unpaidLeaves = attendanceDAO.findApprovedUnpaidLeavesOverlapping(employeeId, monthStart, monthEnd);
        for (LeaveRequest leave : unpaidLeaves) {
            LocalDate start = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(monthEnd) ? monthEnd : leave.getEndDate();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                unpaidDates.add(d);
            }
        }

        List<Attendance> records = attendanceDAO.getAttendanceByEmployeeAndMonth(employeeId, month, year);
        int presentCount = 0;
        int absentCount = 0;
        int halfDayCount = 0;
        BigDecimal extraLop = BigDecimal.ZERO;

        for (Attendance att : records) {
            LocalDate date = att.getAttendanceDate();
            AttendanceStatus status = att.getStatus();
            if (status == AttendanceStatus.PRESENT) {
                presentCount++;
            } else if (status == AttendanceStatus.HALF_DAY) {
                halfDayCount++;
                if (!unpaidDates.contains(date)) {
                    extraLop = extraLop.add(new BigDecimal("0.5"));
                }
            } else if (status == AttendanceStatus.ABSENT) {
                if (!unpaidDates.contains(date)) {
                    absentCount++;
                    extraLop = extraLop.add(BigDecimal.ONE);
                }
            }
            // LEAVE status is informational when unpaid leave already covers LOP
        }

        BigDecimal unpaidBd = BigDecimal.valueOf(unpaidLeaveDays);
        BigDecimal totalLop = unpaidBd.add(extraLop).setScale(2, RoundingMode.HALF_UP);
        if (totalLop.compareTo(BigDecimal.valueOf(workingDays)) > 0) {
            totalLop = BigDecimal.valueOf(workingDays);
        }

        BigDecimal presentDays = BigDecimal.valueOf(workingDays).subtract(totalLop);
        if (presentDays.compareTo(BigDecimal.ZERO) < 0) {
            presentDays = BigDecimal.ZERO;
        }

        // Prefer attendance-derived present count when daily records exist
        if (!records.isEmpty()) {
            BigDecimal fromAttendance = BigDecimal.valueOf(presentCount)
                    .add(BigDecimal.valueOf(halfDayCount).multiply(new BigDecimal("0.5")));
            // Cap by working days after LOP
            if (fromAttendance.compareTo(presentDays) < 0 && totalLop.compareTo(BigDecimal.ZERO) == 0) {
                // Sparse attendance without absences: keep calendar-based presentDays
            } else if (fromAttendance.compareTo(BigDecimal.ZERO) >= 0) {
                presentDays = BigDecimal.valueOf(workingDays).subtract(totalLop).max(BigDecimal.ZERO);
            }
        }

        AttendanceImpact impact = new AttendanceImpact();
        impact.unpaidLeaveDays = unpaidLeaveDays;
        impact.absentDays = absentCount;
        impact.halfDays = halfDayCount;
        impact.presentDaysRecorded = presentCount;
        impact.totalLopDays = totalLop;
        impact.presentDays = presentDays.setScale(0, RoundingMode.HALF_UP).intValue();
        impact.hasAttendanceRecords = !records.isEmpty();
        return impact;
    }

    public static class AttendanceImpact {
        public int unpaidLeaveDays;
        public int absentDays;
        public int halfDays;
        public int presentDaysRecorded;
        public BigDecimal totalLopDays = BigDecimal.ZERO;
        public int presentDays;
        public boolean hasAttendanceRecords;
    }
}
