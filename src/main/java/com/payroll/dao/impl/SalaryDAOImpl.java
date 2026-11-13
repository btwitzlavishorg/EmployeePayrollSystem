package com.payroll.dao.impl;

import com.payroll.dao.SalaryDAO;
import com.payroll.exception.DatabaseException;
import com.payroll.model.EmployeeSalaryComponent;
import com.payroll.model.SalaryComponent;
import com.payroll.model.SalaryStructure;
import com.payroll.model.enums.CalculationType;
import com.payroll.model.enums.ComponentType;
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
import java.util.List;
import java.util.Optional;

public class SalaryDAOImpl implements SalaryDAO {

    @Override
    public Optional<SalaryStructure> getStructureByEmployeeId(int employeeId) {
        String sql = "SELECT * FROM salary_structures WHERE employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                SalaryStructure s = new SalaryStructure();
                s.setStructureId(rs.getInt("structure_id"));
                s.setEmployeeId(rs.getInt("employee_id"));
                s.setBasicSalary(rs.getBigDecimal("basic_salary"));
                Date eff = rs.getDate("effective_date");
                if (eff != null) s.setEffectiveDate(eff.toLocalDate());
                return Optional.of(s);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query salary structure: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean saveOrUpdateStructure(int employeeId, BigDecimal basicSalary, LocalDate effectiveDate) {
        Optional<SalaryStructure> existing = getStructureByEmployeeId(employeeId);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (existing.isPresent()) {
                String sql = "UPDATE salary_structures SET basic_salary = ?, effective_date = ? WHERE employee_id = ?";
                ps = conn.prepareStatement(sql);
                ps.setBigDecimal(1, basicSalary);
                ps.setDate(2, Date.valueOf(effectiveDate));
                ps.setInt(3, employeeId);
            } else {
                String sql = "INSERT INTO salary_structures (employee_id, basic_salary, effective_date) VALUES (?, ?, ?)";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, employeeId);
                ps.setBigDecimal(2, basicSalary);
                ps.setDate(3, Date.valueOf(effectiveDate));
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save/update salary structure: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public List<SalaryComponent> getAllComponents() {
        String sql = "SELECT * FROM salary_components ORDER BY component_type ASC, component_name ASC";
        List<SalaryComponent> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToComponent(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch salary components: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<SalaryComponent> getComponentById(int componentId) {
        String sql = "SELECT * FROM salary_components WHERE component_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, componentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToComponent(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query component by ID: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public Optional<SalaryComponent> getComponentByCode(String code) {
        String sql = "SELECT * FROM salary_components WHERE component_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToComponent(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query component by code: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public int saveComponent(SalaryComponent component) {
        String sql = "INSERT INTO salary_components (component_code, component_name, component_type, " +
                     "calculation_type, default_value, is_active, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, component.getComponentCode());
            ps.setString(2, component.getComponentName());
            ps.setString(3, component.getComponentType().name());
            ps.setString(4, component.getCalculationType().name());
            ps.setBigDecimal(5, component.getDefaultValue());
            ps.setBoolean(6, component.isActive());
            ps.setString(7, component.getDescription());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                component.setComponentId(id);
                return id;
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save salary component: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean updateComponent(SalaryComponent component) {
        String sql = "UPDATE salary_components SET component_name = ?, component_type = ?, " +
                     "calculation_type = ?, default_value = ?, is_active = ?, description = ? WHERE component_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, component.getComponentName());
            ps.setString(2, component.getComponentType().name());
            ps.setString(3, component.getCalculationType().name());
            ps.setBigDecimal(4, component.getDefaultValue());
            ps.setBoolean(5, component.isActive());
            ps.setString(6, component.getDescription());
            ps.setInt(7, component.getComponentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update salary component: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    @Override
    public List<EmployeeSalaryComponent> getEmployeeComponents(int employeeId) {
        String sql = "SELECT esc.*, sc.component_code, sc.component_name, sc.component_type, " +
                     "sc.calculation_type, sc.default_value, sc.is_active, sc.description " +
                     "FROM employee_salary_components esc " +
                     "JOIN salary_components sc ON esc.component_id = sc.component_id " +
                     "WHERE esc.employee_id = ? " +
                     "ORDER BY sc.component_type ASC, sc.component_name ASC";
        List<EmployeeSalaryComponent> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            while (rs.next()) {
                EmployeeSalaryComponent esc = new EmployeeSalaryComponent();
                esc.setId(rs.getInt("id"));
                esc.setEmployeeId(rs.getInt("employee_id"));
                esc.setComponentId(rs.getInt("component_id"));
                esc.setCustomValue(rs.getBigDecimal("custom_value"));
                esc.setEnabled(rs.getBoolean("is_enabled"));

                SalaryComponent sc = new SalaryComponent();
                sc.setComponentId(rs.getInt("component_id"));
                sc.setComponentCode(rs.getString("component_code"));
                sc.setComponentName(rs.getString("component_name"));
                sc.setComponentType(ComponentType.valueOf(rs.getString("component_type")));
                sc.setCalculationType(CalculationType.valueOf(rs.getString("calculation_type")));
                sc.setDefaultValue(rs.getBigDecimal("default_value"));
                sc.setActive(rs.getBoolean("is_active"));
                sc.setDescription(rs.getString("description"));
                esc.setComponent(sc);

                list.add(esc);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch employee salary components: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean saveEmployeeComponent(int employeeId, int componentId, BigDecimal customValue, boolean isEnabled) {
        String checkSql = "SELECT id FROM employee_salary_components WHERE employee_id = ? AND component_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(checkSql);
            ps.setInt(1, employeeId);
            ps.setInt(2, componentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String updateSql = "UPDATE employee_salary_components SET custom_value = ?, is_enabled = ? WHERE id = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setBigDecimal(1, customValue);
                    updatePs.setBoolean(2, isEnabled);
                    updatePs.setInt(3, id);
                    return updatePs.executeUpdate() > 0;
                }
            } else {
                String insertSql = "INSERT INTO employee_salary_components (employee_id, component_id, custom_value, is_enabled) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, employeeId);
                    insertPs.setInt(2, componentId);
                    insertPs.setBigDecimal(3, customValue);
                    insertPs.setBoolean(4, isEnabled);
                    return insertPs.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save employee salary component: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }

    @Override
    public boolean deleteEmployeeComponents(int employeeId) {
        String sql = "DELETE FROM employee_salary_components WHERE employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to remove employee components: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }

    private SalaryComponent mapResultSetToComponent(ResultSet rs) throws SQLException {
        SalaryComponent sc = new SalaryComponent();
        sc.setComponentId(rs.getInt("component_id"));
        sc.setComponentCode(rs.getString("component_code"));
        sc.setComponentName(rs.getString("component_name"));
        sc.setComponentType(ComponentType.valueOf(rs.getString("component_type")));
        sc.setCalculationType(CalculationType.valueOf(rs.getString("calculation_type")));
        sc.setDefaultValue(rs.getBigDecimal("default_value"));
        sc.setActive(rs.getBoolean("is_active"));
        sc.setDescription(rs.getString("description"));
        return sc;
    }
}
