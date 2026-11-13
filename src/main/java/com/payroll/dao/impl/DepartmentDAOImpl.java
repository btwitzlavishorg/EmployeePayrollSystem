package com.payroll.dao.impl;

import com.payroll.dao.DepartmentDAO;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Department;
import com.payroll.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentDAOImpl implements DepartmentDAO {

    @Override
    public List<Department> findAll() {
        String sql = "SELECT d.*, COUNT(e.employee_id) AS emp_count " +
                     "FROM departments d " +
                     "LEFT JOIN employees e ON d.department_id = e.department_id " +
                     "GROUP BY d.department_id " +
                     "ORDER BY d.department_name ASC";
        List<Department> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Department d = new Department();
                d.setDepartmentId(rs.getInt("department_id"));
                d.setDepartmentCode(rs.getString("department_code"));
                d.setDepartmentName(rs.getString("department_name"));
                d.setDescription(rs.getString("description"));
                d.setEmployeeCount(rs.getInt("emp_count"));
                list.add(d);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch departments: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Department> findById(int departmentId) {
        String sql = "SELECT d.*, COUNT(e.employee_id) AS emp_count " +
                     "FROM departments d " +
                     "LEFT JOIN employees e ON d.department_id = e.department_id " +
                     "WHERE d.department_id = ? " +
                     "GROUP BY d.department_id";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Department d = new Department();
                d.setDepartmentId(rs.getInt("department_id"));
                d.setDepartmentCode(rs.getString("department_code"));
                d.setDepartmentName(rs.getString("department_name"));
                d.setDescription(rs.getString("description"));
                d.setEmployeeCount(rs.getInt("emp_count"));
                return Optional.of(d);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query department by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<Department> findByCode(String code) {
        String sql = "SELECT * FROM departments WHERE department_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            rs = ps.executeQuery();
            if (rs.next()) {
                Department d = new Department();
                d.setDepartmentId(rs.getInt("department_id"));
                d.setDepartmentCode(rs.getString("department_code"));
                d.setDepartmentName(rs.getString("department_name"));
                d.setDescription(rs.getString("description"));
                return Optional.of(d);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query department by code: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public int save(Department department) {
        String sql = "INSERT INTO departments (department_code, department_name, description) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, department.getDepartmentCode().trim().toUpperCase());
            ps.setString(2, department.getDepartmentName().trim());
            ps.setString(3, department.getDescription());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                department.setDepartmentId(id);
                return id;
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save department: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean update(Department department) {
        String sql = "UPDATE departments SET department_code = ?, department_name = ?, description = ? WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, department.getDepartmentCode().trim().toUpperCase());
            ps.setString(2, department.getDepartmentName().trim());
            ps.setString(3, department.getDescription());
            ps.setInt(4, department.getDepartmentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update department: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public boolean delete(int departmentId) {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete department: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public int getEmployeeCount(int departmentId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count employees in department: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
}
