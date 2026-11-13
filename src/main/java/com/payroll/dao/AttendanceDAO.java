package com.payroll.dao;

import com.payroll.model.Attendance;
import com.payroll.model.LeaveRequest;
import com.payroll.model.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceDAO {
    boolean markAttendance(Attendance attendance);
    List<Attendance> getAttendanceByDate(LocalDate date);
    List<Attendance> getAttendanceByEmployeeAndMonth(int employeeId, int month, int year);
    int saveLeaveRequest(LeaveRequest request);
    List<LeaveRequest> getAllLeaveRequests();
    List<LeaveRequest> getLeaveRequestsByEmployee(int employeeId);
    boolean updateLeaveStatus(int leaveId, LeaveStatus status, int reviewedBy, String reviewComments);
    int countApprovedUnpaidLeaveDays(int employeeId, int month, int year);
    int countPresentDays(int employeeId, int month, int year);
    int countDaysByStatus(int employeeId, int month, int year, String status);
    java.util.List<LeaveRequest> findApprovedUnpaidLeavesOverlapping(int employeeId, java.time.LocalDate monthStart, java.time.LocalDate monthEnd);
}
