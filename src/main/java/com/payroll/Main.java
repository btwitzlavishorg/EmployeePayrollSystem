package com.payroll;

import com.payroll.util.DatabaseConnection;
import com.payroll.web.PayrollWebServer;

import java.sql.Connection;

/**
 * Main Application Launcher.
 * Bootstraps the database connectivity layer, initializes schemas/seed data,
 * and starts the embedded HTTP web server serving the modern web frontend.
 */
public class Main {

    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println("  Starting Employee Payroll Management System Web Server...");
        System.out.println("====================================================================");

        // 1. Initialize Database Connection and Schema
        try {
            Connection conn = DatabaseConnection.getConnection();
            String dbInfo = DatabaseConnection.getActiveDatabaseInfo();
            String dbUrl = DatabaseConnection.getActiveDatabaseUrl();
            System.out.println("[Database] Status: Connected");
            System.out.println("[Database] Engine: " + dbInfo);
            System.out.println("[Database] URL   : " + dbUrl);
            DatabaseConnection.close(conn);
        } catch (Exception e) {
            System.err.println("[Database] Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Resolve Server Port
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            try { port = Integer.parseInt(envPort); } catch (Exception ignored) {}
        }
        String sysPort = System.getProperty("server.port");
        if (sysPort != null) {
            try { port = Integer.parseInt(sysPort); } catch (Exception ignored) {}
        }

        // 3. Start Embedded Web Server
        try {
            PayrollWebServer webServer = new PayrollWebServer(port);
            webServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));

            System.out.println("====================================================================");
            System.out.println("  EMPLOYEE PAYROLL MANAGEMENT SYSTEM - WEB DASHBOARD");
            System.out.println("====================================================================");
            System.out.println("  Web Portal   : http://localhost:" + port + "/");
            System.out.println("  REST API Base: http://localhost:" + port + "/api/dashboard/stats");
            System.out.println("  Admin Login  : admin / admin123");
            System.out.println("  HR Login     : hrmanager / admin123");
            System.out.println("====================================================================");
        } catch (Exception e) {
            System.err.println("[Main] Critical error starting web server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
