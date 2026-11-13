package com.payroll.web;

import com.payroll.dao.PayrollDAO;
import com.payroll.dao.UserDAO;
import com.payroll.dao.impl.PayrollDAOImpl;
import com.payroll.dao.impl.UserDAOImpl;
import com.payroll.exception.AuthenticationException;
import com.payroll.exception.ValidationException;
import com.payroll.model.*;
import com.payroll.model.enums.*;
import com.payroll.service.*;
import com.payroll.util.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Enterprise Web API Controller.
 * Bridges HTTP REST endpoints directly with the existing Java Service and DAO layers.
 * All payroll calculations, financial rounding, validations, and database mutations
 * remain strictly enforced within this Java backend layer.
 */
public class ApiController {

    private final AuthService authService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final AttendanceService attendanceService;
    private final SalaryService salaryService;
    private final PayrollCalculationService payrollService;
    private final PayrollDAO payrollDAO;
    private final UserDAO userDAO;

    public ApiController() {
        this.authService = new AuthService();
        this.employeeService = new EmployeeService();
        this.departmentService = new DepartmentService();
        this.attendanceService = new AttendanceService();
        this.salaryService = new SalaryService();
        this.payrollService = new PayrollCalculationService();
        this.payrollDAO = new PayrollDAOImpl();
        this.userDAO = new UserDAOImpl();
    }

    // =========================================================================
    // AUTHENTICATION & SESSION
    // =========================================================================

    public Map<String, Object> login(Map<String, Object> body) {
        String username = JsonUtil.getString(body, "username", "");
        String password = JsonUtil.getString(body, "password", "");
        String sessionToken = authService.login(username, password);
        SessionManager.bind(sessionToken);
        User user = SessionManager.getCurrentUser();

        Map<String, Object> userDto = new HashMap<>();
        userDto.put("userId", user.getUserId());
        userDto.put("username", user.getUsername());
        userDto.put("fullName", user.getFullName());
        userDto.put("email", user.getEmail());
        userDto.put("role", user.getRole().name());

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Login successful");
        res.put("sessionToken", sessionToken);
        res.put("user", userDto);
        return res;
    }

    public Map<String, Object> logout() {
        authService.logout();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Logged out successfully");
        return res;
    }

    public Map<String, Object> getCurrentUser() {
        User user = SessionManager.getCurrentUser();
        Map<String, Object> res = new HashMap<>();
        if (user == null) {
            res.put("success", true);
            res.put("authenticated", false);
            return res;
        }
        Map<String, Object> userDto = new HashMap<>();
        userDto.put("userId", user.getUserId());
        userDto.put("username", user.getUsername());
        userDto.put("fullName", user.getFullName());
        userDto.put("email", user.getEmail());
        userDto.put("role", user.getRole().name());
        res.put("authenticated", true);
        res.put("user", userDto);
        res.put("success", true);
        return res;
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    public Map<String, Object> getDashboardStats() {
        DashboardStats stats = payrollDAO.getDashboardStats();
        List<Payroll> recentPayrolls = payrollDAO.getRecentPayrolls(5);
        List<Employee> allEmployees = employeeService.getAllEmployees();

        // Get recent employees (up to 5)
        List<Employee> recentEmployees = new ArrayList<>(allEmployees);
        recentEmployees.sort((a, b) -> Integer.compare(b.getEmployeeId(), a.getEmployeeId()));
        if (recentEmployees.size() > 5) {
            recentEmployees = recentEmployees.subList(0, 5);
        }

        // Calculate historical monthly payroll trend (last 6 months)
        LocalDate now = LocalDate.now();
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            int m = d.getMonthValue();
            int y = d.getYear();
            List<Payroll> monthPayrolls = payrollDAO.findByPeriod(m, y);
            BigDecimal totalNet = BigDecimal.ZERO;
            for (Payroll p : monthPayrolls) {
                totalNet = totalNet.add(p.getNetSalary());
            }
            Map<String, Object> item = new HashMap<>();
            item.put("month", DateTimeUtil.getMonthName(m).substring(0, 3) + " " + y);
            item.put("monthNumber", m);
            item.put("year", y);
            item.put("totalPayroll", totalNet);
            item.put("employeeCount", monthPayrolls.size());
            monthlyTrend.add(item);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("totalEmployees", stats.getTotalEmployees());
        res.put("activeEmployees", stats.getActiveEmployees());
        res.put("onLeaveEmployees", stats.getOnLeaveEmployees());
        res.put("pendingPayrolls", stats.getPendingPayrolls());
        res.put("pendingLeaveRequests", stats.getPendingLeaveRequests());
        res.put("totalMonthlyPayroll", stats.getTotalMonthlyPayroll());
        res.put("departmentDistribution", stats.getDepartmentDistribution());
        res.put("recentPayrolls", recentPayrolls);
        res.put("recentEmployees", recentEmployees);
        res.put("monthlyTrend", monthlyTrend);
        return res;
    }

    // =========================================================================
    // EMPLOYEES
    // =========================================================================

    public List<Employee> getEmployees(Map<String, String> query) {
        String keyword = query.get("keyword");
        String deptStr = query.get("departmentId");
        String statusStr = query.get("status");

        Integer deptId = null;
        if (deptStr != null && !deptStr.trim().isEmpty() && !deptStr.equals("all")) {
            try { deptId = Integer.parseInt(deptStr); } catch (Exception ignored) {}
        }

        EmploymentStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equals("all")) {
            try { status = EmploymentStatus.valueOf(statusStr.toUpperCase()); } catch (Exception ignored) {}
        }

        if ((keyword != null && !keyword.trim().isEmpty()) || deptId != null || status != null) {
            return employeeService.searchEmployees(keyword, deptId, status);
        }
        return employeeService.getAllEmployees();
    }

    public Employee getEmployeeById(int id) {
        return employeeService.getEmployeeById(id)
                .orElseThrow(() -> new ValidationException("Employee with ID " + id + " not found."));
    }

    public Map<String, Object> getNextEmployeeCode() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("nextCode", employeeService.getNextEmployeeCode());
        return res;
    }

    public Map<String, Object> createEmployee(Map<String, Object> body) {
        Employee emp = new Employee();
        emp.setEmployeeCode(JsonUtil.getString(body, "employeeCode", null));
        emp.setFirstName(JsonUtil.getString(body, "firstName", ""));
        emp.setLastName(JsonUtil.getString(body, "lastName", ""));
        emp.setEmail(JsonUtil.getString(body, "email", ""));
        emp.setPhone(JsonUtil.getString(body, "phone", ""));
        emp.setAddress(JsonUtil.getString(body, "address", ""));
        emp.setDepartmentId(JsonUtil.getInt(body, "departmentId", 0));
        emp.setDesignation(JsonUtil.getString(body, "designation", ""));
        emp.setDateOfJoining(JsonUtil.getLocalDate(body, "dateOfJoining"));

        String statusStr = JsonUtil.getString(body, "employmentStatus", "ACTIVE");
        try {
            emp.setEmploymentStatus(EmploymentStatus.valueOf(statusStr.toUpperCase()));
        } catch (Exception e) {
            emp.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }

        emp.setBankName(JsonUtil.getString(body, "bankName", ""));
        emp.setBankAccountNo(JsonUtil.getString(body, "bankAccountNo", ""));
        emp.setBankIfsc(JsonUtil.getString(body, "bankIfsc", ""));
        emp.setPanNo(JsonUtil.getString(body, "panNo", ""));

        BigDecimal initialBasicSalary = JsonUtil.getBigDecimal(body, "basicSalary", BigDecimal.ZERO);

        int empId = employeeService.createEmployee(emp, initialBasicSalary);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("employeeId", empId);
        res.put("message", "Employee " + emp.getFirstName() + " " + emp.getLastName() + " created successfully!");
        return res;
    }

    public Map<String, Object> updateEmployee(int id, Map<String, Object> body) {
        Employee emp = getEmployeeById(id);
        emp.setFirstName(JsonUtil.getString(body, "firstName", emp.getFirstName()));
        emp.setLastName(JsonUtil.getString(body, "lastName", emp.getLastName()));
        emp.setEmail(JsonUtil.getString(body, "email", emp.getEmail()));
        emp.setPhone(JsonUtil.getString(body, "phone", emp.getPhone()));
        emp.setAddress(JsonUtil.getString(body, "address", emp.getAddress()));
        emp.setDepartmentId(JsonUtil.getInt(body, "departmentId", emp.getDepartmentId()));
        emp.setDesignation(JsonUtil.getString(body, "designation", emp.getDesignation()));

        LocalDate doj = JsonUtil.getLocalDate(body, "dateOfJoining");
        if (doj != null) emp.setDateOfJoining(doj);

        String statusStr = JsonUtil.getString(body, "employmentStatus", null);
        if (statusStr != null) {
            try { emp.setEmploymentStatus(EmploymentStatus.valueOf(statusStr.toUpperCase())); } catch (Exception ignored) {}
        }

        emp.setBankName(JsonUtil.getString(body, "bankName", emp.getBankName()));
        emp.setBankAccountNo(JsonUtil.getString(body, "bankAccountNo", emp.getBankAccountNo()));
        emp.setBankIfsc(JsonUtil.getString(body, "bankIfsc", emp.getBankIfsc()));
        emp.setPanNo(JsonUtil.getString(body, "panNo", emp.getPanNo()));

        BigDecimal updatedBasicSalary = JsonUtil.getBigDecimal(body, "basicSalary", null);
        boolean updated = employeeService.updateEmployee(emp, updatedBasicSalary);

        Map<String, Object> res = new HashMap<>();
        res.put("success", updated);
        res.put("message", "Employee updated successfully");
        return res;
    }

    public Map<String, Object> deleteEmployee(int id) {
        boolean deleted = employeeService.deleteEmployee(id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        res.put("message", deleted ? "Employee deleted successfully" : "Employee could not be deleted");
        return res;
    }

    // =========================================================================
    // DEPARTMENTS
    // =========================================================================

    public List<Department> getDepartments() {
        return departmentService.getAllDepartments();
    }

    public Department getDepartmentById(int id) {
        return departmentService.getDepartmentById(id)
                .orElseThrow(() -> new ValidationException("Department with ID " + id + " not found."));
    }

    public Map<String, Object> createDepartment(Map<String, Object> body) {
        Department dept = new Department();
        dept.setDepartmentCode(JsonUtil.getString(body, "departmentCode", ""));
        dept.setDepartmentName(JsonUtil.getString(body, "departmentName", ""));
        dept.setDescription(JsonUtil.getString(body, "description", ""));

        int id = departmentService.createDepartment(dept);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("departmentId", id);
        res.put("message", "Department created successfully");
        return res;
    }

    public Map<String, Object> updateDepartment(int id, Map<String, Object> body) {
        Department dept = getDepartmentById(id);
        dept.setDepartmentCode(JsonUtil.getString(body, "departmentCode", dept.getDepartmentCode()));
        dept.setDepartmentName(JsonUtil.getString(body, "departmentName", dept.getDepartmentName()));
        dept.setDescription(JsonUtil.getString(body, "description", dept.getDescription()));

        boolean updated = departmentService.updateDepartment(dept);
        Map<String, Object> res = new HashMap<>();
        res.put("success", updated);
        res.put("message", "Department updated successfully");
        return res;
    }

    public Map<String, Object> deleteDepartment(int id) {
        boolean deleted = departmentService.deleteDepartment(id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        res.put("message", deleted ? "Department deleted successfully" : "Department could not be deleted");
        return res;
    }

    // =========================================================================
    // ATTENDANCE & LEAVE MANAGEMENT
    // =========================================================================

    public List<Attendance> getAttendance(Map<String, String> query) {
        String dateStr = query.get("date");
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
        return attendanceService.getAttendanceForDate(date);
    }

    public List<Attendance> getEmployeeAttendance(Map<String, String> query) {
        int empId = Integer.parseInt(query.getOrDefault("employeeId", "0"));
        int month = Integer.parseInt(query.getOrDefault("month", String.valueOf(LocalDate.now().getMonthValue())));
        int year = Integer.parseInt(query.getOrDefault("year", String.valueOf(LocalDate.now().getYear())));
        return attendanceService.getEmployeeAttendance(empId, month, year);
    }

    public Map<String, Object> markAttendance(Map<String, Object> body) {
        int empId = JsonUtil.getInt(body, "employeeId", 0);
        if (empId <= 0) throw new ValidationException("Please select an employee.");

        LocalDate date = JsonUtil.getLocalDate(body, "attendanceDate");
        if (date == null) date = LocalDate.now();

        String statusStr = JsonUtil.getString(body, "status", "PRESENT");
        AttendanceStatus status = AttendanceStatus.valueOf(statusStr.toUpperCase());

        LocalTime checkIn = JsonUtil.getLocalTime(body, "checkInTime");
        LocalTime checkOut = JsonUtil.getLocalTime(body, "checkOutTime");
        String notes = JsonUtil.getString(body, "notes", "");

        boolean ok = attendanceService.markAttendance(empId, date, status, checkIn, checkOut, notes);
        Map<String, Object> res = new HashMap<>();
        res.put("success", ok);
        res.put("message", "Attendance marked successfully");
        return res;
    }

    public List<LeaveRequest> getLeaves() {
        return attendanceService.getAllLeaveRequests();
    }

    public Map<String, Object> applyLeave(Map<String, Object> body) {
        int empId = JsonUtil.getInt(body, "employeeId", 0);
        if (empId <= 0) throw new ValidationException("Please select an employee.");

        String typeStr = JsonUtil.getString(body, "leaveType", "CASUAL");
        LeaveType type = LeaveType.valueOf(typeStr.toUpperCase());

        LocalDate start = JsonUtil.getLocalDate(body, "startDate");
        LocalDate end = JsonUtil.getLocalDate(body, "endDate");
        String reason = JsonUtil.getString(body, "reason", "");

        int id = attendanceService.applyLeave(empId, type, start, end, reason);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("leaveId", id);
        res.put("message", "Leave request submitted successfully");
        return res;
    }

    public Map<String, Object> reviewLeave(int leaveId, Map<String, Object> body) {
        String statusStr = JsonUtil.getString(body, "status", "APPROVED");
        LeaveStatus status = LeaveStatus.valueOf(statusStr.toUpperCase());
        String comments = JsonUtil.getString(body, "comments", "");

        int reviewerId = 1; // Default Admin user ID
        User current = SessionManager.getCurrentUser();
        if (current != null) reviewerId = current.getUserId();

        boolean ok = attendanceService.reviewLeave(leaveId, status, reviewerId, comments);
        Map<String, Object> res = new HashMap<>();
        res.put("success", ok);
        res.put("message", "Leave request " + status.name().toLowerCase() + " successfully");
        return res;
    }

    // =========================================================================
    // SALARY STRUCTURE & COMPONENTS
    // =========================================================================

    public List<SalaryComponent> getSalaryComponents() {
        return salaryService.getAllSalaryComponents();
    }

    public Map<String, Object> createSalaryComponent(Map<String, Object> body) {
        SalaryComponent comp = new SalaryComponent();
        comp.setComponentCode(JsonUtil.getString(body, "componentCode", ""));
        comp.setComponentName(JsonUtil.getString(body, "componentName", ""));
        comp.setComponentType(ComponentType.valueOf(JsonUtil.getString(body, "componentType", "EARNING")));
        comp.setCalculationType(CalculationType.valueOf(JsonUtil.getString(body, "calculationType", "FIXED_AMOUNT")));
        comp.setDefaultValue(JsonUtil.getBigDecimal(body, "defaultValue", BigDecimal.ZERO));
        comp.setActive(JsonUtil.getBoolean(body, "active", true));
        comp.setDescription(JsonUtil.getString(body, "description", ""));

        int id = salaryService.createComponent(comp);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("componentId", id);
        res.put("message", "Salary component created successfully");
        return res;
    }

    public Map<String, Object> updateSalaryComponent(int id, Map<String, Object> body) {
        SalaryComponent comp = new SalaryComponent();
        comp.setComponentId(id);
        comp.setComponentName(JsonUtil.getString(body, "componentName", ""));
        comp.setCalculationType(CalculationType.valueOf(JsonUtil.getString(body, "calculationType", "FIXED_AMOUNT")));
        comp.setDefaultValue(JsonUtil.getBigDecimal(body, "defaultValue", BigDecimal.ZERO));
        comp.setActive(JsonUtil.getBoolean(body, "active", true));
        comp.setDescription(JsonUtil.getString(body, "description", ""));

        boolean updated = salaryService.updateComponent(comp);
        Map<String, Object> res = new HashMap<>();
        res.put("success", updated);
        res.put("message", "Salary component updated successfully");
        return res;
    }

    public Map<String, Object> getEmployeeSalaryStructure(int employeeId) {
        Employee emp = getEmployeeById(employeeId);
        Optional<SalaryStructure> structOpt = salaryService.getStructureByEmployeeId(employeeId);
        BigDecimal basicSalary = structOpt.map(SalaryStructure::getBasicSalary).orElse(BigDecimal.ZERO);

        List<EmployeeSalaryComponent> empComponents = salaryService.getEmployeeComponents(employeeId);
        List<SalaryComponent> allComponents = salaryService.getAllSalaryComponents();

        // Construct complete list of components with effective state for this employee
        List<Map<String, Object>> componentsList = new ArrayList<>();
        for (SalaryComponent globalComp : allComponents) {
            Map<String, Object> item = new HashMap<>();
            item.put("componentId", globalComp.getComponentId());
            item.put("componentCode", globalComp.getComponentCode());
            item.put("componentName", globalComp.getComponentName());
            item.put("componentType", globalComp.getComponentType().name());
            item.put("calculationType", globalComp.getCalculationType().name());
            item.put("defaultValue", globalComp.getDefaultValue());
            item.put("description", globalComp.getDescription());

            // Check if mapped for employee
            Optional<EmployeeSalaryComponent> mapped = empComponents.stream()
                    .filter(c -> c.getComponentId() == globalComp.getComponentId())
                    .findFirst();

            if (mapped.isPresent()) {
                item.put("enabled", mapped.get().isEnabled());
                item.put("customValue", mapped.get().getCustomValue());
                item.put("effectiveValue", mapped.get().getEffectiveValue());
            } else {
                item.put("enabled", globalComp.isActive());
                item.put("customValue", null);
                item.put("effectiveValue", globalComp.getDefaultValue());
            }

            // Calculate current live line-item amount based on basic salary
            BigDecimal effVal = (BigDecimal) item.get("effectiveValue");
            BigDecimal calculatedAmount = BigDecimal.ZERO;
            if (globalComp.getCalculationType() == CalculationType.PERCENTAGE_OF_BASIC) {
                calculatedAmount = basicSalary.multiply(effVal).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                calculatedAmount = effVal.setScale(2, RoundingMode.HALF_UP);
            }
            item.put("calculatedAmount", calculatedAmount);

            componentsList.add(item);
        }

        // Compute authoritative preview of totals from Java backend
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        for (Map<String, Object> c : componentsList) {
            if (Boolean.TRUE.equals(c.get("enabled"))) {
                BigDecimal amt = (BigDecimal) c.get("calculatedAmount");
                if ("EARNING".equals(c.get("componentType"))) {
                    totalAllowances = totalAllowances.add(amt);
                } else if ("DEDUCTION".equals(c.get("componentType"))) {
                    totalDeductions = totalDeductions.add(amt);
                }
            }
        }
        BigDecimal grossSalary = basicSalary.add(totalAllowances);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) netSalary = BigDecimal.ZERO;

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("employeeId", employeeId);
        res.put("employeeCode", emp.getEmployeeCode());
        res.put("employeeName", emp.getFullName());
        res.put("departmentName", emp.getDepartmentName());
        res.put("designation", emp.getDesignation());
        res.put("basicSalary", basicSalary);
        res.put("components", componentsList);
        res.put("totalAllowances", totalAllowances);
        res.put("grossSalary", grossSalary);
        res.put("totalDeductions", totalDeductions);
        res.put("netSalary", netSalary);
        return res;
    }

    public Map<String, Object> updateEmployeeSalaryStructure(int employeeId, Map<String, Object> body) {
        BigDecimal basicSalary = JsonUtil.getBigDecimal(body, "basicSalary", BigDecimal.ZERO);
        salaryService.updateEmployeeBasicSalary(employeeId, basicSalary);

        // Update components if provided
        Object componentsObj = body.get("components");
        if (componentsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> components = (List<Map<String, Object>>) componentsObj;
            for (Map<String, Object> compMap : components) {
                int compId = JsonUtil.getInt(compMap, "componentId", 0);
                boolean enabled = JsonUtil.getBoolean(compMap, "enabled", true);
                BigDecimal customVal = JsonUtil.getBigDecimal(compMap, "customValue", null);
                if (compId > 0) {
                    salaryService.saveEmployeeComponent(employeeId, compId, customVal, enabled);
                }
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Salary structure updated successfully");
        return res;
    }

    // =========================================================================
    // PAYROLL PROCESSING & PREVIEW
    // =========================================================================

    public Map<String, Object> calculateDraftPayrollPreview(Map<String, Object> body) {
        int employeeId = JsonUtil.getInt(body, "employeeId", 0);
        int month = JsonUtil.getInt(body, "payrollMonth", LocalDate.now().getMonthValue());
        int year = JsonUtil.getInt(body, "payrollYear", LocalDate.now().getYear());
        BigDecimal adjustment = JsonUtil.getBigDecimal(body, "manualAdjustment", BigDecimal.ZERO);
        String reason = JsonUtil.getString(body, "adjustmentReason", "");

        Employee emp = getEmployeeById(employeeId);
        Payroll draft = payrollService.calculateDraftPayroll(emp, month, year, adjustment, reason);

        // Check if duplicate already exists in DB
        Optional<Payroll> existing = payrollDAO.findByEmployeeAndPeriod(employeeId, month, year);
        boolean isDuplicate = existing.isPresent();

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("isDuplicate", isDuplicate);
        if (isDuplicate) {
            res.put("existingStatus", existing.get().getStatus().getDisplayName());
            res.put("existingPayrollId", existing.get().getPayrollId());
        }
        res.put("payroll", draft);
        return res;
    }

    public Map<String, Object> processPayroll(Map<String, Object> body) {
        int employeeId = JsonUtil.getInt(body, "employeeId", 0);
        int month = JsonUtil.getInt(body, "payrollMonth", 0);
        int year = JsonUtil.getInt(body, "payrollYear", 0);
        BigDecimal adjustment = JsonUtil.getBigDecimal(body, "manualAdjustment", BigDecimal.ZERO);
        String reason = JsonUtil.getString(body, "adjustmentReason", "");

        if (employeeId <= 0 || month <= 0 || year <= 0) {
            throw new ValidationException("Please provide a valid Employee, Month, and Year.");
        }

        int payrollId = payrollService.processEmployeePayroll(employeeId, month, year, adjustment, reason);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("payrollId", payrollId);
        res.put("message", "Payroll successfully generated and saved for " + DateTimeUtil.getMonthName(month) + " " + year);
        return res;
    }

    public Map<String, Object> batchProcessPayroll(Map<String, Object> body) {
        int month = JsonUtil.getInt(body, "payrollMonth", LocalDate.now().getMonthValue());
        int year = JsonUtil.getInt(body, "payrollYear", LocalDate.now().getYear());

        List<Payroll> processed = payrollService.processMonthlyBatchPayroll(month, year);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("processedCount", processed.size());
        res.put("message", "Batch payroll generation completed: " + processed.size() + " employee payroll(s) generated for " + DateTimeUtil.getMonthName(month) + " " + year);
        return res;
    }

    public List<Payroll> getPayrolls(Map<String, String> query) {
        String monthStr = query.get("month");
        String yearStr = query.get("year");
        int month = (monthStr != null && !monthStr.isEmpty()) ? Integer.parseInt(monthStr) : LocalDate.now().getMonthValue();
        int year = (yearStr != null && !yearStr.isEmpty()) ? Integer.parseInt(yearStr) : LocalDate.now().getYear();

        return payrollService.getPayrollsByPeriod(month, year);
    }

    public Payroll getPayrollById(int payrollId) {
        return payrollService.getPayrollById(payrollId)
                .orElseThrow(() -> new ValidationException("Payroll record with ID " + payrollId + " not found."));
    }

    public Map<String, Object> markPayrollPaid(int payrollId, Map<String, Object> body) {
        LocalDate payDate = JsonUtil.getLocalDate(body, "paymentDate");
        if (payDate == null) payDate = LocalDate.now();

        String method = JsonUtil.getString(body, "paymentMethod", "BANK_TRANSFER");
        String ref = JsonUtil.getString(body, "paymentReference", "NEFT-" + System.currentTimeMillis());

        boolean ok = payrollService.markAsPaid(payrollId, payDate, method, ref);
        Map<String, Object> res = new HashMap<>();
        res.put("success", ok);
        res.put("message", "Payroll marked as PAID successfully");
        return res;
    }

    public Map<String, Object> deletePayroll(int payrollId) {
        boolean ok = payrollService.deletePayroll(payrollId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", ok);
        res.put("message", ok ? "Payroll deleted successfully" : "Could not delete payroll");
        return res;
    }

    // =========================================================================
    // PAYSLIP PDF STREAMING
    // =========================================================================

    public byte[] generatePayslipPdfBytes(int payrollId) throws Exception {
        Payroll payroll = getPayrollById(payrollId);
        Employee employee = getEmployeeById(payroll.getEmployeeId());

        File tempFile = File.createTempFile("payslip_" + payrollId + "_", ".pdf");
        try {
            PdfGenerator.generatePayslipPdf(payroll, employee, tempFile);
            return Files.readAllBytes(tempFile.toPath());
        } finally {
            tempFile.delete();
        }
    }

    // =========================================================================
    // REPORTS & ANALYTICS
    // =========================================================================

    public Map<String, Object> getMonthlyReport(int month, int year) {
        List<Payroll> payrolls = payrollService.getPayrollsByPeriod(month, year);
        BigDecimal totalBasic = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        Map<String, BigDecimal> deptNetTotals = new HashMap<>();
        Map<String, Integer> deptEmpCounts = new HashMap<>();

        for (Payroll p : payrolls) {
            totalBasic = totalBasic.add(p.getBasicSalary());
            totalAllowances = totalAllowances.add(p.getTotalAllowances());
            totalGross = totalGross.add(p.getGrossSalary());
            totalDeductions = totalDeductions.add(p.getTotalDeductions());
            totalNet = totalNet.add(p.getNetSalary());

            String dept = p.getDepartmentName() != null ? p.getDepartmentName() : "General";
            deptNetTotals.put(dept, deptNetTotals.getOrDefault(dept, BigDecimal.ZERO).add(p.getNetSalary()));
            deptEmpCounts.put(dept, deptEmpCounts.getOrDefault(dept, 0) + 1);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("month", month);
        res.put("monthName", DateTimeUtil.getMonthName(month));
        res.put("year", year);
        res.put("totalEmployees", payrolls.size());
        res.put("totalBasic", totalBasic);
        res.put("totalAllowances", totalAllowances);
        res.put("totalGross", totalGross);
        res.put("totalDeductions", totalDeductions);
        res.put("totalNet", totalNet);
        res.put("departmentTotals", deptNetTotals);
        res.put("departmentCounts", deptEmpCounts);
        res.put("records", payrolls);
        return res;
    }

    public Map<String, Object> getAnnualReport(int year) {
        List<Payroll> payrolls = payrollDAO.getAnnualPayrolls(year);
        List<Map<String, Object>> monthData = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            final int monthVal = m;
            BigDecimal netTotal = BigDecimal.ZERO;
            BigDecimal grossTotal = BigDecimal.ZERO;
            BigDecimal dedTotal = BigDecimal.ZERO;
            int count = 0;

            for (Payroll p : payrolls) {
                if (p.getPayrollMonth() == monthVal) {
                    netTotal = netTotal.add(p.getNetSalary());
                    grossTotal = grossTotal.add(p.getGrossSalary());
                    dedTotal = dedTotal.add(p.getTotalDeductions());
                    count++;
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("month", monthVal);
            item.put("monthName", DateTimeUtil.getMonthName(monthVal));
            item.put("employeeCount", count);
            item.put("grossSalary", grossTotal);
            item.put("totalDeductions", dedTotal);
            item.put("netSalary", netTotal);
            monthData.add(item);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("year", year);
        res.put("months", monthData);
        res.put("totalRecords", payrolls.size());
        return res;
    }

    public byte[] exportReportCsvBytes(String reportType, int month, int year) throws Exception {
        String[] headers;
        List<String[]> rows = new ArrayList<>();

        if ("annual".equalsIgnoreCase(reportType)) {
            headers = new String[]{"Month", "Employees Paid", "Gross Salary (INR)", "Deductions (INR)", "Net Payout (INR)"};
            Map<String, Object> annual = getAnnualReport(year);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> months = (List<Map<String, Object>>) annual.get("months");
            for (Map<String, Object> m : months) {
                rows.add(new String[]{
                        String.valueOf(m.get("monthName")),
                        String.valueOf(m.get("employeeCount")),
                        String.valueOf(m.get("grossSalary")),
                        String.valueOf(m.get("totalDeductions")),
                        String.valueOf(m.get("netSalary"))
                });
            }
        } else {
            headers = new String[]{"Employee Code", "Employee Name", "Department", "Designation", "Working Days", "Present Days", "LOP Days", "Basic Salary (INR)", "Allowances (INR)", "Gross (INR)", "Deductions (INR)", "Net Salary (INR)", "Status"};
            List<Payroll> payrolls = payrollService.getPayrollsByPeriod(month, year);
            for (Payroll p : payrolls) {
                rows.add(new String[]{
                        p.getEmployeeCode(),
                        p.getEmployeeName(),
                        p.getDepartmentName(),
                        p.getDesignation(),
                        String.valueOf(p.getWorkingDays()),
                        String.valueOf(p.getPresentDays()),
                        String.valueOf(p.getUnpaidLeaveDays()),
                        p.getBasicSalary().toPlainString(),
                        p.getTotalAllowances().toPlainString(),
                        p.getGrossSalary().toPlainString(),
                        p.getTotalDeductions().toPlainString(),
                        p.getNetSalary().toPlainString(),
                        p.getStatus().getDisplayName()
                });
            }
        }

        File tempFile = File.createTempFile("report_", ".csv");
        try {
            CsvExporter.exportDataToCsv(headers, rows, tempFile);
            return Files.readAllBytes(tempFile.toPath());
        } finally {
            tempFile.delete();
        }
    }

    public byte[] exportReportPdfBytes(String reportType, int month, int year) throws Exception {
        String title;
        String subtitle;
        String[] headers;
        List<String[]> rows = new ArrayList<>();

        if ("annual".equalsIgnoreCase(reportType)) {
            title = "ANNUAL PAYROLL SUMMARY REPORT - " + year;
            subtitle = "Consolidated Month-by-Month Expenditure Statement";
            headers = new String[]{"Month", "Employees Paid", "Gross Salary (INR)", "Deductions (INR)", "Net Payout (INR)"};
            Map<String, Object> annual = getAnnualReport(year);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> months = (List<Map<String, Object>>) annual.get("months");
            for (Map<String, Object> m : months) {
                rows.add(new String[]{
                        String.valueOf(m.get("monthName")),
                        String.valueOf(m.get("employeeCount")),
                        CurrencyFormatter.format((BigDecimal) m.get("grossSalary")),
                        CurrencyFormatter.format((BigDecimal) m.get("totalDeductions")),
                        CurrencyFormatter.format((BigDecimal) m.get("netSalary"))
                });
            }
        } else {
            title = "MONTHLY PAYROLL REGISTER - " + DateTimeUtil.getMonthName(month).toUpperCase() + " " + year;
            subtitle = "Official Salary and Statutory Deduction Disbursement Register";
            headers = new String[]{"Emp Code", "Employee Name", "Department", "Designation", "Present/Work Days", "Basic Salary", "Allowances", "Gross Salary", "Deductions", "Net Salary"};
            List<Payroll> payrolls = payrollService.getPayrollsByPeriod(month, year);
            for (Payroll p : payrolls) {
                rows.add(new String[]{
                        p.getEmployeeCode(),
                        p.getEmployeeName(),
                        p.getDepartmentName(),
                        p.getDesignation(),
                        p.getPresentDays() + " / " + p.getWorkingDays(),
                        CurrencyFormatter.format(p.getBasicSalary()),
                        CurrencyFormatter.format(p.getTotalAllowances()),
                        CurrencyFormatter.format(p.getGrossSalary()),
                        CurrencyFormatter.format(p.getTotalDeductions()),
                        CurrencyFormatter.format(p.getNetSalary())
                });
            }
        }

        File tempFile = File.createTempFile("report_", ".pdf");
        try {
            PdfGenerator.generateReportPdf(title, subtitle, headers, rows, tempFile);
            return Files.readAllBytes(tempFile.toPath());
        } finally {
            tempFile.delete();
        }
    }

    // =========================================================================
    // SYSTEM SETTINGS & METADATA
    // =========================================================================

    public Map<String, Object> getSettingsInfo() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("appName", "Employee Payroll Management System");
        res.put("appVersion", "1.0.0");
        res.put("companyName", "PayrollCorp Technologies Pvt. Ltd.");
        res.put("companyEmail", "payroll@payrollcorp.com");
        res.put("companyPhone", "+91 (080) 4567-8900");
        res.put("companyAddress", "Tech Innovation Hub, Block 4, Silicon City, Bengaluru, Karnataka 560100");
        res.put("currencySymbol", "₹");
        res.put("currencyCode", "INR");
        res.put("databaseMode", DatabaseConnection.getActiveDatabaseInfo());
        res.put("databaseUrl", DatabaseConnection.getActiveDatabaseUrl());
        res.put("javaVersion", System.getProperty("java.version"));
        res.put("osName", System.getProperty("os.name"));

        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        res.put("memoryUsage", (totalMem - freeMem) + " MB / " + totalMem + " MB");
        return res;
    }

    public Map<String, Object> changePassword(Map<String, Object> body) {
        String currentPassword = JsonUtil.getString(body, "currentPassword", "");
        String newPassword = JsonUtil.getString(body, "newPassword", "");

        if (newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            throw new AuthenticationException("You must be logged in to change your password.");
        }
        int userId = current.getUserId();

        Optional<User> userOpt = userDAO.findById(userId);
        if (!userOpt.isPresent()) {
            throw new ValidationException("User not found.");
        }

        User u = userOpt.get();
        if (!PasswordHasher.verify(currentPassword, u.getPasswordHash(), u.getSalt())) {
            throw new ValidationException("Current password does not match.");
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPassword, newSalt);
        userDAO.updatePassword(userId, newHash, newSalt);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Password updated successfully");
        return res;
    }
}
