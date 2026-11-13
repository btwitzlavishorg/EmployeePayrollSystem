package com.payroll.web;

import com.payroll.exception.AuthenticationException;
import com.payroll.exception.DuplicateEntityException;
import com.payroll.exception.ValidationException;
import com.payroll.service.SessionManager;
import com.payroll.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Enterprise HTTP REST API Request Dispatcher.
 * Dispatches incoming /api/* calls to ApiController methods, manages exception handling,
 * returns standard JSON responses, and streams dynamic PDF/CSV binary documents.
 */
public class ApiHandler implements HttpHandler {

    public static final String SESSION_COOKIE = "PAYROLL_SESSION";

    private final ApiController controller;

    public ApiHandler(ApiController controller) {
        this.controller = controller;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.length() > 5 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String sessionToken = extractSessionToken(exchange);
        if (sessionToken != null) {
            SessionManager.bind(sessionToken);
        }

        try {
            Map<String, Object> body = new HashMap<>();
            if ("POST".equals(method) || "PUT".equals(method)) {
                String bodyStr = readRequestBody(exchange);
                if (bodyStr != null && !bodyStr.trim().isEmpty()) {
                    body = JsonUtil.parseObject(bodyStr);
                }
            }

            enforceAccessControl(method, path);
            dispatch(exchange, method, path, query, body);

        } catch (AuthenticationException e) {
            sendJsonError(exchange, 401, e.getMessage());
        } catch (ValidationException | DuplicateEntityException e) {
            sendJsonError(exchange, 400, e.getMessage());
        } catch (NoSuchElementException e) {
            sendJsonError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            System.err.println("[ApiHandler] Server error on " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            sendJsonError(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            SessionManager.clearRequest();
        }
    }

    private void enforceAccessControl(String method, String path) {
        if ((path.equals("/api/auth/login") && "POST".equals(method))
                || (path.equals("/api/auth/me") && "GET".equals(method))) {
            return;
        }

        if (!SessionManager.isLoggedIn()) {
            throw new AuthenticationException("Authentication required. Please log in.");
        }

        // HR / Admin write operations
        boolean hrWrite =
                (path.equals("/api/employees") && "POST".equals(method))
                || (path.startsWith("/api/employees/") && ("PUT".equals(method) || "DELETE".equals(method)))
                || (path.equals("/api/departments") && "POST".equals(method))
                || (path.startsWith("/api/departments/") && ("PUT".equals(method) || "DELETE".equals(method)))
                || (path.equals("/api/salary/components") && "POST".equals(method))
                || (path.startsWith("/api/salary/components/") && "PUT".equals(method))
                || (path.startsWith("/api/salary/structure/") && ("PUT".equals(method) || "POST".equals(method)))
                || (path.equals("/api/attendance/mark") && "POST".equals(method))
                || (path.startsWith("/api/leaves/") && path.endsWith("/review") && "POST".equals(method));

        if (hrWrite && !SessionManager.canManageHr()) {
            throw new AuthenticationException("Insufficient privileges. HR Manager or Admin role required.");
        }

        // Payroll processing / payment mutations
        boolean payrollWrite =
                (path.equals("/api/payroll/process") && "POST".equals(method))
                || (path.equals("/api/payroll/batch") && "POST".equals(method))
                || (path.startsWith("/api/payrolls/") && path.endsWith("/pay") && "POST".equals(method))
                || (path.startsWith("/api/payrolls/") && "DELETE".equals(method));

        if (payrollWrite && !SessionManager.canManagePayroll()) {
            throw new AuthenticationException("Insufficient privileges. Payroll access required.");
        }
    }

    private void dispatch(HttpExchange exchange, String method, String path, Map<String, String> query, Map<String, Object> body) throws Exception {

        // --- AUTH ---
        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            Map<String, Object> res = controller.login(body);
            String token = String.valueOf(res.get("sessionToken"));
            setSessionCookie(exchange, token, false);
            sendJsonResponse(exchange, 200, res);
            return;
        }
        if (path.equals("/api/auth/logout") && "POST".equals(method)) {
            Map<String, Object> res = controller.logout();
            setSessionCookie(exchange, "", true);
            sendJsonResponse(exchange, 200, res);
            return;
        }
        if (path.equals("/api/auth/me") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getCurrentUser());
            return;
        }

        // --- DASHBOARD ---
        if (path.equals("/api/dashboard/stats") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getDashboardStats());
            return;
        }

        // --- EMPLOYEES ---
        if (path.equals("/api/employees/next-code") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getNextEmployeeCode());
            return;
        }
        if (path.equals("/api/employees") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getEmployees(query));
            return;
        }
        if (path.equals("/api/employees") && "POST".equals(method)) {
            sendJsonResponse(exchange, 201, controller.createEmployee(body));
            return;
        }
        if (path.startsWith("/api/employees/")) {
            String sub = path.substring("/api/employees/".length());
            int id = Integer.parseInt(sub);
            if ("GET".equals(method)) {
                sendJsonResponse(exchange, 200, controller.getEmployeeById(id));
                return;
            }
            if ("PUT".equals(method)) {
                sendJsonResponse(exchange, 200, controller.updateEmployee(id, body));
                return;
            }
            if ("DELETE".equals(method)) {
                sendJsonResponse(exchange, 200, controller.deleteEmployee(id));
                return;
            }
        }

        // --- DEPARTMENTS ---
        if (path.equals("/api/departments") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getDepartments());
            return;
        }
        if (path.equals("/api/departments") && "POST".equals(method)) {
            sendJsonResponse(exchange, 201, controller.createDepartment(body));
            return;
        }
        if (path.startsWith("/api/departments/")) {
            int id = Integer.parseInt(path.substring("/api/departments/".length()));
            if ("GET".equals(method)) {
                sendJsonResponse(exchange, 200, controller.getDepartmentById(id));
                return;
            }
            if ("PUT".equals(method)) {
                sendJsonResponse(exchange, 200, controller.updateDepartment(id, body));
                return;
            }
            if ("DELETE".equals(method)) {
                sendJsonResponse(exchange, 200, controller.deleteDepartment(id));
                return;
            }
        }

        // --- ATTENDANCE & LEAVES ---
        if (path.equals("/api/attendance") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getAttendance(query));
            return;
        }
        if (path.equals("/api/attendance/employee") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getEmployeeAttendance(query));
            return;
        }
        if (path.equals("/api/attendance/mark") && "POST".equals(method)) {
            sendJsonResponse(exchange, 200, controller.markAttendance(body));
            return;
        }
        if (path.equals("/api/leaves") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getLeaves());
            return;
        }
        if (path.equals("/api/leaves/apply") && "POST".equals(method)) {
            sendJsonResponse(exchange, 201, controller.applyLeave(body));
            return;
        }
        if (path.startsWith("/api/leaves/") && path.endsWith("/review") && "POST".equals(method)) {
            String mid = path.substring("/api/leaves/".length(), path.length() - "/review".length());
            int id = Integer.parseInt(mid);
            sendJsonResponse(exchange, 200, controller.reviewLeave(id, body));
            return;
        }

        // --- SALARY STRUCTURE & COMPONENTS ---
        if (path.equals("/api/salary/components") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getSalaryComponents());
            return;
        }
        if (path.equals("/api/salary/components") && "POST".equals(method)) {
            sendJsonResponse(exchange, 201, controller.createSalaryComponent(body));
            return;
        }
        if (path.startsWith("/api/salary/components/") && "PUT".equals(method)) {
            int id = Integer.parseInt(path.substring("/api/salary/components/".length()));
            sendJsonResponse(exchange, 200, controller.updateSalaryComponent(id, body));
            return;
        }
        if (path.startsWith("/api/salary/structure/")) {
            int empId = Integer.parseInt(path.substring("/api/salary/structure/".length()));
            if ("GET".equals(method)) {
                sendJsonResponse(exchange, 200, controller.getEmployeeSalaryStructure(empId));
                return;
            }
            if ("PUT".equals(method) || "POST".equals(method)) {
                sendJsonResponse(exchange, 200, controller.updateEmployeeSalaryStructure(empId, body));
                return;
            }
        }

        // --- PAYROLL PROCESSING & PREVIEW ---
        if (path.equals("/api/payroll/calculate-preview") && "POST".equals(method)) {
            sendJsonResponse(exchange, 200, controller.calculateDraftPayrollPreview(body));
            return;
        }
        if (path.equals("/api/payroll/process") && "POST".equals(method)) {
            sendJsonResponse(exchange, 201, controller.processPayroll(body));
            return;
        }
        if (path.equals("/api/payroll/batch") && "POST".equals(method)) {
            sendJsonResponse(exchange, 200, controller.batchProcessPayroll(body));
            return;
        }
        if (path.equals("/api/payrolls") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getPayrolls(query));
            return;
        }
        if (path.startsWith("/api/payrolls/") && path.endsWith("/pdf") && "GET".equals(method)) {
            String mid = path.substring("/api/payrolls/".length(), path.length() - "/pdf".length());
            int id = Integer.parseInt(mid);
            byte[] pdfBytes = controller.generatePayslipPdfBytes(id);
            sendBinaryResponse(exchange, 200, "application/pdf", "inline; filename=\"Payslip_" + id + ".pdf\"", pdfBytes);
            return;
        }
        if (path.startsWith("/api/payrolls/") && path.endsWith("/pay") && "POST".equals(method)) {
            String mid = path.substring("/api/payrolls/".length(), path.length() - "/pay".length());
            int id = Integer.parseInt(mid);
            sendJsonResponse(exchange, 200, controller.markPayrollPaid(id, body));
            return;
        }
        if (path.startsWith("/api/payrolls/")) {
            int id = Integer.parseInt(path.substring("/api/payrolls/".length()));
            if ("GET".equals(method)) {
                sendJsonResponse(exchange, 200, controller.getPayrollById(id));
                return;
            }
            if ("DELETE".equals(method)) {
                sendJsonResponse(exchange, 200, controller.deletePayroll(id));
                return;
            }
        }

        // --- REPORTS & EXPORTS ---
        if (path.equals("/api/reports/monthly") && "GET".equals(method)) {
            int m = Integer.parseInt(query.getOrDefault("month", String.valueOf(LocalDate.now().getMonthValue())));
            int y = Integer.parseInt(query.getOrDefault("year", String.valueOf(LocalDate.now().getYear())));
            sendJsonResponse(exchange, 200, controller.getMonthlyReport(m, y));
            return;
        }
        if (path.equals("/api/reports/annual") && "GET".equals(method)) {
            int y = Integer.parseInt(query.getOrDefault("year", String.valueOf(LocalDate.now().getYear())));
            sendJsonResponse(exchange, 200, controller.getAnnualReport(y));
            return;
        }
        if (path.equals("/api/reports/export/csv") && "GET".equals(method)) {
            String type = query.getOrDefault("type", "monthly");
            int m = Integer.parseInt(query.getOrDefault("month", String.valueOf(LocalDate.now().getMonthValue())));
            int y = Integer.parseInt(query.getOrDefault("year", String.valueOf(LocalDate.now().getYear())));
            byte[] csvBytes = controller.exportReportCsvBytes(type, m, y);
            sendBinaryResponse(exchange, 200, "text/csv; charset=UTF-8", "attachment; filename=\"payroll_report_" + type + ".csv\"", csvBytes);
            return;
        }
        if (path.equals("/api/reports/export/pdf") && "GET".equals(method)) {
            String type = query.getOrDefault("type", "monthly");
            int m = Integer.parseInt(query.getOrDefault("month", String.valueOf(LocalDate.now().getMonthValue())));
            int y = Integer.parseInt(query.getOrDefault("year", String.valueOf(LocalDate.now().getYear())));
            byte[] pdfBytes = controller.exportReportPdfBytes(type, m, y);
            sendBinaryResponse(exchange, 200, "application/pdf", "attachment; filename=\"payroll_report_" + type + ".pdf\"", pdfBytes);
            return;
        }

        // --- SETTINGS ---
        if (path.equals("/api/settings/info") && "GET".equals(method)) {
            sendJsonResponse(exchange, 200, controller.getSettingsInfo());
            return;
        }
        if (path.equals("/api/settings/password") && "POST".equals(method)) {
            sendJsonResponse(exchange, 200, controller.changePassword(body));
            return;
        }

        sendJsonError(exchange, 404, "Endpoint not found: " + method + " " + path);
    }

    private void setSessionCookie(HttpExchange exchange, String token, boolean clear) {
        String cookie;
        if (clear || token == null || token.isEmpty()) {
            cookie = SESSION_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        } else {
            cookie = SESSION_COOKIE + "=" + token + "; Path=/; Max-Age=43200; HttpOnly; SameSite=Lax";
        }
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    private String extractSessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) {
            String[] parts = header.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith(SESSION_COOKIE + "=")) {
                    return trimmed.substring((SESSION_COOKIE + "=").length());
                }
            }
        }
        // Also accept Authorization: Bearer <token> for API clients
        List<String> auth = exchange.getRequestHeaders().get("Authorization");
        if (auth != null && !auth.isEmpty()) {
            String value = auth.get(0).trim();
            if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return value.substring(7).trim();
            }
        }
        return null;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }

    private void sendJsonError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("error", message);
        sendJsonResponse(exchange, statusCode, err);
    }

    private void sendBinaryResponse(HttpExchange exchange, int statusCode, String contentType, String disposition, byte[] data) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (disposition != null) {
            exchange.getResponseHeaders().set("Content-Disposition", disposition);
        }
        exchange.sendResponseHeaders(statusCode, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
        exchange.close();
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:" + exchange.getLocalAddress().getPort());
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return query;
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    query.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                              URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                } else if (!pair.isEmpty()) {
                    query.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return query;
    }
}
