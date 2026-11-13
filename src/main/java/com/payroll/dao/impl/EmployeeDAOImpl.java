package com.payroll.dao.impl;

import com.payroll.dao.EmployeeDAO;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Employee;
import com.payroll.model.enums.EmploymentStatus;
import com.payroll.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDAOImpl implements EmployeeDAO {

    private static final String BASE_SELECT =
            "SELECT e.*, d.department_name, s.basic_salary " +
            "FROM employees e " +
            "JOIN departments d ON e.department_id = d.department_id " +
            "LEFT JOIN salary_structures s ON e.employee_id = s.employee_id ";

    @Override
    public List<Employee> findAll() {
        String sql = BASE_SELECT + "ORDER BY e.employee_code ASC";
        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch employees: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Employee> findById(int employeeId) {
        String sql = BASE_SELECT + "WHERE e.employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEmployee(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query employee by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Employee> findByCode(String employeeCode) {
        String sql = BASE_SELECT + "WHERE e.employee_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, employeeCode);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEmployee(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query employee by code: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        String sql = BASE_SELECT + "WHERE LOWER(e.email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEmployee(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query employee by email: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public List<Employee> search(String keyword, Integer departmentId, EmploymentStatus status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(e.employee_code) LIKE ? OR LOWER(e.first_name) LIKE ? OR LOWER(e.last_name) LIKE ? OR LOWER(e.designation) LIKE ?) ");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (departmentId != null && departmentId > 0) {
            sql.append("AND e.department_id = ? ");
            params.add(departmentId);
        }

        if (status != null) {
            sql.append("AND e.employment_status = ? ");
            params.add(status.name());
        }

        sql.append("ORDER BY e.employee_code ASC");

        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to filter employees: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public int save(Employee employee) {
        String sql = "INSERT INTO employees (employee_code, first_name, last_name, email, phone, address, " +
                     "department_id, designation, date_of_joining, employment_status, bank_name, " +
                     "bank_account_no, bank_ifsc, pan_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, employee.getEmployeeCode());
            ps.setString(2, employee.getFirstName());
            ps.setString(3, employee.getLastName());
            ps.setString(4, employee.getEmail());
            ps.setString(5, employee.getPhone());
            ps.setString(6, employee.getAddress());
            ps.setInt(7, employee.getDepartmentId());
            ps.setString(8, employee.getDesignation());
            ps.setDate(9, Date.valueOf(employee.getDateOfJoining()));
            ps.setString(10, employee.getEmploymentStatus().name());
            ps.setString(11, employee.getBankName());
            ps.setString(12, employee.getBankAccountNo());
            ps.setString(13, employee.getBankIfsc());
            ps.setString(14, employee.getPanNo());

            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                employee.setEmployeeId(id);
                return id;
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save employee: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean update(Employee employee) {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?, " +
                     "department_id = ?, designation = ?, date_of_joining = ?, employment_status = ?, " +
                     "bank_name = ?, bank_account_no = ?, bank_ifsc = ?, pan_no = ? WHERE employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, employee.getFirstName());
            ps.setString(2, employee.getLastName());
            ps.setString(3, employee.getEmail());
            ps.setString(4, employee.getPhone());
            ps.setString(5, employee.getAddress());
            ps.setInt(6, employee.getDepartmentId());
            ps.setString(7, employee.getDesignation());
            ps.setDate(8, Date.valueOf(employee.getDateOfJoining()));
            ps.setString(9, employee.getEmploymentStatus().name());
            ps.setString(10, employee.getBankName());
            ps.setString(11, employee.getBankAccountNo());
            ps.setString(12, employee.getBankIfsc());
            ps.setString(13, employee.getPanNo());
            ps.setInt(14, employee.getEmployeeId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update employee: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public boolean delete(int employeeId) {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete employee: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public String generateNextEmployeeCode() {
        String sql = "SELECT employee_code FROM employees ORDER BY employee_id DESC LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                String lastCode = rs.getString("employee_code");
                if (lastCode != null && lastCode.toUpperCase().startsWith("EMP")) {
                    try {
                        int num = Integer.parseInt(lastCode.substring(3));
                        return String.format("EMP%04d", num + 1);
                    } catch (NumberFormatException ignored) {}
                }
            }
            return "EMP1001";
        } catch (SQLException e) {
            return "EMP1001";
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setEmployeeId(rs.getInt("employee_id"));
        emp.setEmployeeCode(rs.getString("employee_code"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        emp.setAddress(rs.getString("address"));
        emp.setDepartmentId(rs.getInt("department_id"));
        emp.setDepartmentName(rs.getString("department_name"));
        emp.setDesignation(rs.getString("designation"));
        Date doj = rs.getDate("date_of_joining");
        if (doj != null) emp.setDateOfJoining(doj.toLocalDate());
        try {
            emp.setEmploymentStatus(EmploymentStatus.valueOf(rs.getString("employment_status")));
        } catch (Exception e) {
            emp.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }
        emp.setBankName(rs.getString("bank_name"));
        emp.setBankAccountNo(rs.getString("bank_account_no"));
        emp.setBankIfsc(rs.getString("bank_ifsc"));
        emp.setPanNo(rs.getString("pan_no"));

        java.math.BigDecimal basic = rs.getBigDecimal("basic_salary");
        emp.setBasicSalary(basic != null ? basic : java.math.BigDecimal.ZERO);
        return emp;
    }
}
