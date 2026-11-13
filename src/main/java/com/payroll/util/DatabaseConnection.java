package com.payroll.util;

import com.payroll.exception.DatabaseException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Enterprise-grade Database Connection Manager.
 * 
 * Manages JDBC connectivity with support for:
 * 1. Environment variables & properties file configuration.
 * 2. Primary MySQL database (localhost:3306/payroll_db).
 * 3. Graceful fallback to local embedded database (H2 in MySQL mode)
 *    when MySQL daemon is not running on localhost, guaranteeing zero-configuration
 *    local execution and seamless viva presentation.
 * 4. Automatic schema and sample data deployment.
 */
public class DatabaseConnection {

    private static final Properties props = new Properties();
    private static boolean isInitialized = false;
    private static boolean usingMySQL = false;
    private static String activeDatabaseUrl = "";

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        // Load default properties from classpath
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[DatabaseConnection] Warning: Could not load classpath db.properties: " + e.getMessage());
        }

        // Check if config/db.properties exists in current working directory
        File localConfigFile = new File("config/db.properties");
        if (localConfigFile.exists()) {
            try (FileInputStream fis = new FileInputStream(localConfigFile)) {
                props.load(fis);
            } catch (Exception e) {
                System.err.println("[DatabaseConnection] Warning: Could not load config/db.properties: " + e.getMessage());
            }
        }

        // Environment variable overrides
        String envHost = System.getenv("DB_HOST");
        String envPort = System.getenv("DB_PORT");
        String envName = System.getenv("DB_NAME");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        if (envHost != null && envName != null) {
            String port = (envPort != null) ? envPort : "3306";
            String mysqlUrl = "jdbc:mysql://" + envHost + ":" + port + "/" + envName +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
            props.setProperty("db.url", mysqlUrl);
        }
        if (envUser != null) props.setProperty("db.user", envUser);
        if (envPass != null) props.setProperty("db.password", envPass);
    }

    /**
     * Obtains an active JDBC connection.
     */
    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = null;

        // Try primary MySQL configuration
        String mysqlUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
        String mysqlUser = props.getProperty("db.user", "root");
        String mysqlPass = props.getProperty("db.password", "");
        String mysqlDriver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        try {
            Class.forName(mysqlDriver);
            conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass);
            usingMySQL = true;
            activeDatabaseUrl = mysqlUrl;
        } catch (Exception ex) {
            // Check fallback setting
            boolean allowFallback = Boolean.parseBoolean(props.getProperty("db.allow.fallback", "true"));
            if (!allowFallback) {
                throw new DatabaseException("Failed to connect to MySQL database at " + mysqlUrl + ": " + ex.getMessage(), ex);
            }

            // Fallback to embedded H2 database running in MySQL compatibility mode.
            // Default data dir is under the user home so the app works outside the project folder.
            String fallbackUrl = resolveFallbackUrl(props.getProperty("db.fallback.url", ""));
            String fallbackUser = props.getProperty("db.fallback.user", "sa");
            String fallbackPass = props.getProperty("db.fallback.password", "");
            String fallbackDriver = props.getProperty("db.fallback.driver", "org.h2.Driver");

            try {
                Class.forName(fallbackDriver);
                conn = DriverManager.getConnection(fallbackUrl, fallbackUser, fallbackPass);
                usingMySQL = false;
                activeDatabaseUrl = fallbackUrl;
            } catch (Exception fallbackEx) {
                throw new DatabaseException("Failed to connect to both MySQL and Embedded database: " + fallbackEx.getMessage(), fallbackEx);
            }
        }

        // Automatically initialize database schema and seed data on first successful connection
        if (!isInitialized) {
            boolean autoInit = Boolean.parseBoolean(props.getProperty("db.auto.init", "true"));
            if (autoInit) {
                DatabaseInitializer.initializeDatabase(conn);
            }
            isInitialized = true;
        }

        return conn;
    }

    public static boolean isUsingMySQL() {
        return usingMySQL;
    }

    public static String getActiveDatabaseInfo() {
        return usingMySQL ? "MySQL Server (Production Mode)" : "Embedded MySQL-Compatible Engine (Local Fallback Mode)";
    }

    public static String getActiveDatabaseUrl() {
        return activeDatabaseUrl;
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    public static void close(Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        close(stmt, rs);
        close(conn);
    }

    /**
     * Resolves H2 file URL. Prefer configured absolute path; otherwise use
     * ~/.payroll-system/data so runs outside the project folder still work.
     */
    private static String resolveFallbackUrl(String configured) {
        String envDataDir = System.getenv("PAYROLL_DATA_DIR");
        String sysDataDir = System.getProperty("payroll.data.dir");
        File dataDir;
        if (envDataDir != null && !envDataDir.trim().isEmpty()) {
            dataDir = new File(envDataDir.trim());
        } else if (sysDataDir != null && !sysDataDir.trim().isEmpty()) {
            dataDir = new File(sysDataDir.trim());
        } else if (configured != null && configured.contains("jdbc:h2:./data/")) {
            dataDir = new File(System.getProperty("user.home"), ".payroll-system/data");
        } else if (configured != null && configured.startsWith("jdbc:h2:")) {
            return configured;
        } else {
            dataDir = new File(System.getProperty("user.home"), ".payroll-system/data");
        }

        if (!dataDir.exists() && !dataDir.mkdirs()) {
            System.err.println("[DatabaseConnection] Warning: could not create data directory " + dataDir.getAbsolutePath());
        }

        String dbFile = new File(dataDir, "payroll_db").getAbsolutePath().replace('\\', '/');
        return "jdbc:h2:file:" + dbFile + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
    }
}
