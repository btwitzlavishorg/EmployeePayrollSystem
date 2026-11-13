package com.payroll.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Automates the creation of database tables, constraints, and sample data.
 * Prefers classpath resources so fat-JAR runs work outside the project folder.
 */
public class DatabaseInitializer {

    public static void initializeDatabase(Connection conn) {
        try {
            if (isDatabasePopulated(conn)) {
                return;
            }

            System.out.println("[DatabaseInitializer] Initializing database schema...");
            executeSqlScript(conn, "database/schema.sql");

            System.out.println("[DatabaseInitializer] Populating initial seed data...");
            executeSqlScript(conn, "database/sample_data.sql");

            System.out.println("[DatabaseInitializer] Database initialization completed successfully!");
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isDatabasePopulated(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM employees LIMIT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void executeSqlScript(Connection conn, String scriptPath) {
        StringBuilder sql = new StringBuilder();

        try (BufferedReader reader = openScriptReader(scriptPath)) {
            if (reader == null) {
                System.err.println("[DatabaseInitializer] SQL script not found: " + scriptPath);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                    continue;
                }
                sql.append(line).append("\n");
                if (trimmed.endsWith(";")) {
                    String stmtStr = sql.toString().trim();
                    if (stmtStr.endsWith(";")) {
                        stmtStr = stmtStr.substring(0, stmtStr.length() - 1);
                    }
                    if (!stmtStr.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(stmtStr);
                        } catch (SQLException ex) {
                            if (!ex.getMessage().toLowerCase().contains("already exists")) {
                                System.err.println("[DatabaseInitializer] Warning on statement: " + ex.getMessage());
                            }
                        }
                    }
                    sql.setLength(0);
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Failed to execute script " + scriptPath + ": " + e.getMessage());
        }
    }

    private static BufferedReader openScriptReader(String scriptPath) throws Exception {
        // 1) Classpath (preferred for packaged JAR)
        InputStream classpathStream = DatabaseInitializer.class.getResourceAsStream("/" + scriptPath);
        if (classpathStream != null) {
            return new BufferedReader(new InputStreamReader(classpathStream, StandardCharsets.UTF_8));
        }

        // 2) Project-relative path (dev mode)
        File file = new File(scriptPath);
        if (file.exists()) {
            return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        }

        // 3) Beside the running JAR
        try {
            File jarDir = new File(DatabaseInitializer.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            File besideJar = new File(jarDir, scriptPath);
            if (besideJar.exists()) {
                return Files.newBufferedReader(besideJar.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        return null;
    }
}
