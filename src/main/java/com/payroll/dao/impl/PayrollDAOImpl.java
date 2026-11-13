package com.payroll.dao.impl;

import com.payroll.dao.PayrollDAO;
import com.payroll.exception.DatabaseException;
import com.payroll.model.DashboardStats;
import com.payroll.model.Payroll;
import com.payroll.model.PayrollDetail;
import com.payroll.model.enums.ComponentType;
import com.payroll.model.enums.PayrollStatus;
import com.payroll.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PayrollDAOImpl implements PayrollDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, " +
            "d.department_name, e.designation, e.bank_name, e.bank_account_no, e.pan_no " +
            "FROM payrolls p " +
            "JOIN employees e ON p.employee_id = e.employee_id " +
            "JOIN departments d ON e.department_id = d.department_id ";

    @Override
    public int savePayroll(Payroll p) {
        String insertPayrollSql =
                "INSERT INTO payrolls (employee_id, payroll_month, payroll_year, basic_salary, " +
                "total_allowances, gross_salary, total_deductions, net_salary, working_days, " +
                "present_days, unpaid_leave_days, status, payment_date, payment_method, " +
                "payment_reference, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertDetailSql =
                "INSERT INTO payroll_details (payroll_id, component_name, component_type, amount) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psPayroll = null;
        PreparedStatement psDetail = null;
        ResultSet rsKeys = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Enable manual transaction control

            psPayroll = conn.prepareStatement(insertPayrollSql, Statement.RETURN_GENERATED_KEYS);
            psPayroll.setInt(1, p.getEmployeeId());
            psPayroll.setInt(2, p.getPayrollMonth());
            psPayroll.setInt(3, p.getPayrollYear());
            psPayroll.setBigDecimal(4, p.getBasicSalary());
            psPayroll.setBigDecimal(5, p.getTotalAllowances());
            psPayroll.setBigDecimal(6, p.getGrossSalary());
            psPayroll.setBigDecimal(7, p.getTotalDeductions());
            psPayroll.setBigDecimal(8, p.getNetSalary());
            psPayroll.setInt(9, p.getWorkingDays());
            psPayroll.setInt(10, p.getPresentDays());
            psPayroll.setInt(11, p.getUnpaidLeaveDays());
            psPayroll.setString(12, p.getStatus().name());
            psPayroll.setDate(13, p.getPaymentDate() != null ? Date.valueOf(p.getPaymentDate()) : null);
            psPayroll.setString(14, p.getPaymentMethod());
            psPayroll.setString(15, p.getPaymentReference());
            psPayroll.setString(16, p.getNotes());

            psPayroll.executeUpdate();
            rsKeys = psPayroll.getGeneratedKeys();
            int payrollId = 0;
            if (rsKeys.next()) {
                payrollId = rsKeys.getInt(1);
                p.setPayrollId(payrollId);
            }

            if (payrollId > 0 && p.getDetails() != null && !p.getDetails().isEmpty()) {
                psDetail = conn.prepareStatement(insertDetailSql);
                for (PayrollDetail d : p.getDetails()) {
                    psDetail.setInt(1, payrollId);
                    psDetail.setString(2, d.getComponentName());
                    psDetail.setString(3, d.getComponentType().name());
                    psDetail.setBigDecimal(4, d.getAmount());
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            conn.commit();
            return payrollId;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseException("Failed to save payroll record: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            DatabaseConnection.close(psPayroll, rsKeys);
            DatabaseConnection.close(psDetail, null);
            DatabaseConnection.close(conn);
        }
    }

    @Override
    public boolean updatePayroll(Payroll p) {
        String sql = "UPDATE payrolls SET basic_salary = ?, total_allowances = ?, gross_salary = ?, " +
                     "total_deductions = ?, net_salary = ?, working_days = ?, present_days = ?, " +
                     "unpaid_leave_days = ?, status = ?, payment_date = ?, payment_method = ?, " +
                     "payment_reference = ?, notes = ? WHERE payroll_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setBigDecimal(1, p.getBasicSalary());
            ps.setBigDecimal(2, p.getTotalAllowances());
            ps.setBigDecimal(3, p.getGrossSalary());
            ps.setBigDecimal(4, p.getTotalDeductions());
            ps.setBigDecimal(5, p.getNetSalary());
            ps.setInt(6, p.getWorkingDays());
            ps.setInt(7, p.getPresentDays());
            ps.setInt(8, p.getUnpaidLeaveDays());
            ps.setString(9, p.getStatus().name());
            ps.setDate(10, p.getPaymentDate() != null ? Date.valueOf(p.getPaymentDate()) : null);
            ps.setString(11, p.getPaymentMethod());
            ps.setString(12, p.getPaymentReference());
            ps.setString(13, p.getNotes());
            ps.setInt(14, p.getPayrollId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payroll: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public boolean updateStatus(int payrollId, PayrollStatus status, LocalDate paymentDate, String paymentMethod, String ref) {
        String sql = "UPDATE payrolls SET status = ?, payment_date = ?, payment_method = ?, payment_reference = ? WHERE payroll_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            ps.setDate(2, paymentDate != null ? Date.valueOf(paymentDate) : null);
            ps.setString(3, paymentMethod);
            ps.setString(4, ref);
            ps.setInt(5, payrollId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payroll status: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public boolean delete(int payrollId) {
        String sql = "DELETE FROM payrolls WHERE payroll_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, payrollId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete payroll: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public Optional<Payroll> findById(int payrollId) {
        String sql = BASE_SELECT + "WHERE p.payroll_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, payrollId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Payroll p = mapResultSetToPayroll(rs);
                p.setDetails(loadDetails(conn, payrollId));
                return Optional.of(p);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query payroll by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Payroll> findByEmployeeAndPeriod(int employeeId, int month, int year) {
        String sql = BASE_SELECT + "WHERE p.employee_id = ? AND p.payroll_month = ? AND p.payroll_year = ?";
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
            if (rs.next()) {
                Payroll p = mapResultSetToPayroll(rs);
                p.setDetails(loadDetails(conn, p.getPayrollId()));
                return Optional.of(p);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query payroll by period: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<Payroll> findByPeriod(int month, int year) {
        String sql = BASE_SELECT + "WHERE p.payroll_month = ? AND p.payroll_year = ? ORDER BY e.employee_code ASC";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, month);
            ps.setInt(2, year);
            rs = ps.executeQuery();
            while (rs.next()) {
                Payroll p = mapResultSetToPayroll(rs);
                p.setDetails(loadDetails(conn, p.getPayrollId()));
                list.add(p);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch payrolls for period: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<Payroll> getRecentPayrolls(int limit) {
        String sql = BASE_SELECT + "ORDER BY p.payroll_year DESC, p.payroll_month DESC, p.created_at DESC LIMIT ?";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                Payroll p = mapResultSetToPayroll(rs);
                list.add(p);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch recent payrolls: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();

            // Total, Active, On Leave employees
            rs = stmt.executeQuery("SELECT " +
                    "COUNT(*) AS total, " +
                    "SUM(CASE WHEN employment_status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_cnt, " +
                    "SUM(CASE WHEN employment_status = 'ON_LEAVE' THEN 1 ELSE 0 END) AS leave_cnt " +
                    "FROM employees");
            if (rs.next()) {
                stats.setTotalEmployees(rs.getInt("total"));
                stats.setActiveEmployees(rs.getInt("active_cnt"));
                stats.setOnLeaveEmployees(rs.getInt("leave_cnt"));
            }
            rs.close();

            // Latest month total payroll expense
            rs = stmt.executeQuery("SELECT COALESCE(SUM(net_salary), 0) FROM payrolls " +
                    "WHERE payroll_month = (SELECT MAX(payroll_month) FROM payrolls WHERE payroll_year = (SELECT MAX(payroll_year) FROM payrolls)) " +
                    "AND payroll_year = (SELECT MAX(payroll_year) FROM payrolls)");
            if (rs.next()) {
                stats.setTotalMonthlyPayroll(rs.getBigDecimal(1));
            }
            rs.close();

            // Pending Leave Requests
            rs = stmt.executeQuery("SELECT COUNT(*) FROM leave_requests WHERE status = 'PENDING'");
            if (rs.next()) {
                stats.setPendingLeaveRequests(rs.getInt(1));
            }
            rs.close();

            // Unpaid / awaiting-payment payrolls (generated but not yet marked PAID)
            rs = stmt.executeQuery("SELECT COUNT(*) FROM payrolls WHERE status IN ('PENDING', 'PROCESSED')");
            if (rs.next()) {
                stats.setPendingPayrolls(rs.getInt(1));
            }
            rs.close();

            // Department distribution
            Map<String, Integer> dist = new HashMap<>();
            rs = stmt.executeQuery("SELECT d.department_name, COUNT(e.employee_id) AS cnt " +
                    "FROM departments d " +
                    "LEFT JOIN employees e ON d.department_id = e.department_id " +
                    "GROUP BY d.department_name ORDER BY cnt DESC");
            while (rs.next()) {
                dist.put(rs.getString("department_name"), rs.getInt("cnt"));
            }
            stats.setDepartmentDistribution(dist);

            return stats;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch dashboard stats: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, stmt, rs);
        }
    }

    @Override
    public List<Payroll> getAnnualPayrolls(int year) {
        String sql = BASE_SELECT + "WHERE p.payroll_year = ? ORDER BY p.payroll_month ASC, e.employee_code ASC";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToPayroll(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch annual payroll report: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    private List<PayrollDetail> loadDetails(Connection conn, int payrollId) {
        String sql = "SELECT * FROM payroll_details WHERE payroll_id = ? ORDER BY component_type ASC, component_name ASC";
        List<PayrollDetail> details = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payrollId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PayrollDetail d = new PayrollDetail();
                    d.setDetailId(rs.getInt("detail_id"));
                    d.setPayrollId(payrollId);
                    d.setComponentName(rs.getString("component_name"));
                    d.setComponentType(ComponentType.valueOf(rs.getString("component_type")));
                    d.setAmount(rs.getBigDecimal("amount"));
                    details.add(d);
                }
            }
        } catch (SQLException ignored) {}
        return details;
    }

    private Payroll mapResultSetToPayroll(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setPayrollId(rs.getInt("payroll_id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        p.setEmployeeCode(rs.getString("employee_code"));
        p.setEmployeeName(rs.getString("emp_name"));
        p.setDepartmentName(rs.getString("department_name"));
        p.setDesignation(rs.getString("designation"));
        p.setBankName(rs.getString("bank_name"));
        p.setBankAccountNo(rs.getString("bank_account_no"));
        p.setPanNo(rs.getString("pan_no"));

        p.setPayrollMonth(rs.getInt("payroll_month"));
        p.setPayrollYear(rs.getInt("payroll_year"));
        p.setBasicSalary(rs.getBigDecimal("basic_salary"));
        p.setTotalAllowances(rs.getBigDecimal("total_allowances"));
        p.setGrossSalary(rs.getBigDecimal("gross_salary"));
        p.setTotalDeductions(rs.getBigDecimal("total_deductions"));
        p.setNetSalary(rs.getBigDecimal("net_salary"));

        p.setWorkingDays(rs.getInt("working_days"));
        p.setPresentDays(rs.getInt("present_days"));
        p.setUnpaidLeaveDays(rs.getInt("unpaid_leave_days"));

        p.setStatus(PayrollStatus.valueOf(rs.getString("status")));
        Date payDate = rs.getDate("payment_date");
        if (payDate != null) p.setPaymentDate(payDate.toLocalDate());
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPaymentReference(rs.getString("payment_reference"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
