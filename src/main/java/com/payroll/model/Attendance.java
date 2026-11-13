package com.payroll.model;

import com.payroll.model.enums.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public class Attendance {
    private int attendanceId;
    private int employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate attendanceDate;
    private AttendanceStatus status = AttendanceStatus.PRESENT;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String notes;

    public Attendance() {}

    public Attendance(int employeeId, LocalDate attendanceDate, AttendanceStatus status,
                      LocalTime checkInTime, LocalTime checkOutTime, String notes) {
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.notes = notes;
    }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
