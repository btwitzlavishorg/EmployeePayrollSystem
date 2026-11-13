package com.payroll.model;

import java.math.BigDecimal;
import java.util.Map;

public class DashboardStats {
    private int totalEmployees;
    private int activeEmployees;
    private int onLeaveEmployees;
    private BigDecimal totalMonthlyPayroll = BigDecimal.ZERO;
    private int pendingLeaveRequests;
    private int pendingPayrolls;
    private Map<String, Integer> departmentDistribution;

    public DashboardStats() {}

    public int getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }

    public int getActiveEmployees() { return activeEmployees; }
    public void setActiveEmployees(int activeEmployees) { this.activeEmployees = activeEmployees; }

    public int getOnLeaveEmployees() { return onLeaveEmployees; }
    public void setOnLeaveEmployees(int onLeaveEmployees) { this.onLeaveEmployees = onLeaveEmployees; }

    public BigDecimal getTotalMonthlyPayroll() { return totalMonthlyPayroll; }
    public void setTotalMonthlyPayroll(BigDecimal totalMonthlyPayroll) { this.totalMonthlyPayroll = totalMonthlyPayroll; }

    public int getPendingLeaveRequests() { return pendingLeaveRequests; }
    public void setPendingLeaveRequests(int pendingLeaveRequests) { this.pendingLeaveRequests = pendingLeaveRequests; }

    public int getPendingPayrolls() { return pendingPayrolls; }
    public void setPendingPayrolls(int pendingPayrolls) { this.pendingPayrolls = pendingPayrolls; }

    public Map<String, Integer> getDepartmentDistribution() { return departmentDistribution; }
    public void setDepartmentDistribution(Map<String, Integer> departmentDistribution) { this.departmentDistribution = departmentDistribution; }
}
