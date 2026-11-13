package com.payroll.dao.impl;

import com.payroll.dao.AttendanceDAO;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Attendance;
import com.payroll.model.LeaveRequest;
import com.payroll.model.enums.AttendanceStatus;
import com.payroll.model.enums.LeaveStatus;
import com.payroll.model.enums.LeaveType;
import com.payroll.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAOImpl implements AttendanceDAO {

    @Override
    public boolean markAttendance(Attendance att) {
        String checkSql = "SELECT attendance_id FROM attendance WHERE employee_id = ? AND attendance_date = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, att.getEmployeeId());
            ps.setDate(2, Date.valueOf(att.getAttendanceDate()));
            rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("attendance_id");
                String updateSql = "UPDATE attendance SET status = ?, check_in_time = ?, check_out_time = ?, notes = ? WHERE attendance_id = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setString(1, att.getStatus().name());
                    updatePs.setTime(2, att.getCheckInTime() != null ? Time.valueOf(att.getCheckInTime()) : null);
                    updatePs.setTime(3, att.getCheckOutTime() != null ? Time.valueOf(att.getCheckOutTime()) : null);
                    updatePs.setString(4, att.getNotes());
                    updatePs.setInt(5, id);
                    return updatePs.executeUpdate() > 0;
                }
            } else {
                String insertSql = "INSERT INTO attendance (employee_id, attendance_date, status, check_in_time, check_out_time, notes) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, att.getEmployeeId());
                    insertPs.setDate(2, Date.valueOf(att.getAttendanceDate()));
                    insertPs.setString(3, att.getStatus().name());
                    insertPs.setTime(4, att.getCheckInTime() != null ? Time.valueOf(att.getCheckInTime()) : null);
                    insertPs.setTime(5, att.getCheckOutTime() != null ? Time.valueOf(att.getCheckOutTime()) : null);
                    insertPs.setString(6, att.getNotes());
                    return insertPs.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record attendance: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        String sql = "SELECT a.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name " +
                     "FROM attendance a " +
                     "JOIN employees e ON a.employee_id = e.employee_id " +
                     "WHERE a.attendance_date = ? ORDER BY e.employee_code ASC";
        List<Attendance> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(date));
            rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance();
                a.setAttendanceId(rs.getInt("attendance_id"));
                a.setEmployeeId(rs.getInt("employee_id"));
                a.setEmployeeCode(rs.getString("employee_code"));
                a.setEmployeeName(rs.getString("emp_name"));
                a.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
                a.setStatus(AttendanceStatus.valueOf(rs.getString("status")));
                Time in = rs.getTime("check_in_time");
                if (in != null) a.setCheckInTime(in.toLocalTime());
                Time out = rs.getTime("check_out_time");
                if (out != null) a.setCheckOutTime(out.toLocalTime());
                a.setNotes(rs.getString("notes"));
                list.add(a);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch attendance for date: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<Attendance> getAttendanceByEmployeeAndMonth(int employeeId, int month, int year) {
        String sql = "SELECT a.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name " +
                     "FROM attendance a " +
                     "JOIN employees e ON a.employee_id = e.employee_id " +
                     "WHERE a.employee_id = ? AND MONTH(a.attendance_date) = ? AND YEAR(a.attendance_date) = ? " +
                     "ORDER BY a.attendance_date ASC";
        List<Attendance> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance();
                a.setAttendanceId(rs.getInt("attendance_id"));
                a.setEmployeeId(rs.getInt("employee_id"));
                a.setEmployeeCode(rs.getString("employee_code"));
                a.setEmployeeName(rs.getString("emp_name"));
                a.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
                a.setStatus(AttendanceStatus.valueOf(rs.getString("status")));
                Time in = rs.getTime("check_in_time");
                if (in != null) a.setCheckInTime(in.toLocalTime());
                Time out = rs.getTime("check_out_time");
                if (out != null) a.setCheckOutTime(out.toLocalTime());
                a.setNotes(rs.getString("notes"));
                list.add(a);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch monthly employee attendance: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public int saveLeaveRequest(LeaveRequest req) {
        String sql = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, total_days, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, req.getEmployeeId());
            ps.setString(2, req.getLeaveType().name());
            ps.setDate(3, Date.valueOf(req.getStartDate()));
            ps.setDate(4, Date.valueOf(req.getEndDate()));
            ps.setInt(5, req.getTotalDays());
            ps.setString(6, req.getReason());
            ps.setString(7, req.getStatus().name());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                req.setLeaveId(id);
                return id;
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save leave request: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        String sql = "SELECT lr.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, u.full_name AS reviewer_name " +
                     "FROM leave_requests lr " +
                     "JOIN employees e ON lr.employee_id = e.employee_id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.user_id " +
                     "ORDER BY lr.applied_at DESC";
        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLeave(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch leave requests: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<LeaveRequest> getLeaveRequestsByEmployee(int employeeId) {
        String sql = "SELECT lr.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, u.full_name AS reviewer_name " +
                     "FROM leave_requests lr " +
                     "JOIN employees e ON lr.employee_id = e.employee_id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.user_id " +
                     "WHERE lr.employee_id = ? ORDER BY lr.applied_at DESC";
        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLeave(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch employee leave requests: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean updateLeaveStatus(int leaveId, LeaveStatus status, int reviewedBy, String reviewComments) {
        String sql = "UPDATE leave_requests SET status = ?, reviewed_by = ?, reviewed_at = ?, review_comments = ? WHERE leave_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            ps.setInt(2, reviewedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, reviewComments);
            ps.setInt(5, leaveId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update leave request status: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public int countApprovedUnpaidLeaveDays(int employeeId, int month, int year) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<LeaveRequest> leaves = findApprovedUnpaidLeavesOverlapping(employeeId, monthStart, monthEnd);
        int days = 0;
        for (LeaveRequest leave : leaves) {
            LocalDate start = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(monthEnd) ? monthEnd : leave.getEndDate();
            if (!end.isBefore(start)) {
                days += (int) (end.toEpochDay() - start.toEpochDay() + 1);
            }
        }
        return days;
    }

    @Override
    public List<LeaveRequest> findApprovedUnpaidLeavesOverlapping(int employeeId, LocalDate monthStart, LocalDate monthEnd) {
        String sql = "SELECT lr.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, " +
                     "u.full_name AS reviewer_name " +
                     "FROM leave_requests lr " +
                     "JOIN employees e ON lr.employee_id = e.employee_id " +
                     "LEFT JOIN users u ON lr.reviewed_by = u.user_id " +
                     "WHERE lr.employee_id = ? AND lr.leave_type = 'UNPAID' AND lr.status = 'APPROVED' " +
                     "AND lr.start_date <= ? AND lr.end_date >= ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<LeaveRequest> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(monthEnd));
            ps.setDate(3, Date.valueOf(monthStart));
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLeave(rs));
            }
            return list;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] Failed counting unpaid leave overlap: " + e.getMessage());
            return list;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public int countPresentDays(int employeeId, int month, int year) {
        return countDaysByStatus(employeeId, month, year, "PRESENT");
    }

    @Override
    public int countDaysByStatus(int employeeId, int month, int year, String status) {
        String sql = "SELECT COUNT(*) FROM attendance " +
                     "WHERE employee_id = ? AND status = ? " +
                     "AND MONTH(attendance_date) = ? AND YEAR(attendance_date) = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setString(2, status);
            ps.setInt(3, month);
            ps.setInt(4, year);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] Failed counting attendance status " + status + ": " + e.getMessage());
            return 0;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    private LeaveRequest mapResultSetToLeave(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setLeaveId(rs.getInt("leave_id"));
        r.setEmployeeId(rs.getInt("employee_id"));
        r.setEmployeeCode(rs.getString("employee_code"));
        r.setEmployeeName(rs.getString("emp_name"));
        r.setLeaveType(LeaveType.valueOf(rs.getString("leave_type")));
        r.setStartDate(rs.getDate("start_date").toLocalDate());
        r.setEndDate(rs.getDate("end_date").toLocalDate());
        r.setTotalDays(rs.getInt("total_days"));
        r.setReason(rs.getString("reason"));
        r.setStatus(LeaveStatus.valueOf(rs.getString("status")));
        Timestamp applied = rs.getTimestamp("applied_at");
        if (applied != null) r.setAppliedAt(applied.toLocalDateTime());
        int rev = rs.getInt("reviewed_by");
        if (!rs.wasNull()) r.setReviewedBy(rev);
        r.setReviewerName(rs.getString("reviewer_name"));
        Timestamp reviewed = rs.getTimestamp("reviewed_at");
        if (reviewed != null) r.setReviewedAt(reviewed.toLocalDateTime());
        r.setReviewComments(rs.getString("review_comments"));
        return r;
    }
}
