package com.hostelconnect.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static String host;
    private static String port;
    private static String dbName;
    private static String username;
    private static String password;
    private static String sslMode;

    static {
        loadConfiguration();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Ensure mysql-connector-j is included.", e);
        }
    }

    public static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading from classpath
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {}

        // 2. Try loading directly from file system (src/main/resources/db.properties)
        File srcFile = new File("src/main/resources/db.properties");
        if (srcFile.exists()) {
            try (InputStream in = new FileInputStream(srcFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        // 3. Try loading directly from root db.properties
        File rootFile = new File("db.properties");
        if (rootFile.exists()) {
            try (InputStream in = new FileInputStream(rootFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        // Environment variables take precedence if set, otherwise fallback to properties
        host = getVal(System.getenv("TIDB_HOST"), props.getProperty("db.host", "gateway01.ap-southeast-1.prod.aws.tidbcloud.com"));
        port = getVal(System.getenv("TIDB_PORT"), props.getProperty("db.port", "4000"));
        dbName = getVal(System.getenv("TIDB_DATABASE"), props.getProperty("db.name", "hostelconnect"));
        username = getVal(System.getenv("TIDB_USER"), props.getProperty("db.username", "2DL4cWRn4GTLSek.root"));
        password = getVal(System.getenv("TIDB_PASSWORD"), props.getProperty("db.password", ""));
        sslMode = getVal(System.getenv("TIDB_SSL_MODE"), props.getProperty("db.sslMode", "REQUIRED"));
    }

    private static String getVal(String envVal, String propVal) {
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return propVal != null ? propVal.trim() : "";
    }

    public static Connection getConnection() throws SQLException {
        loadConfiguration();

        if (password == null || password.isEmpty() || "YOUR_TIDB_PASSWORD_HERE".equals(password)) {
            throw new SQLException("Database password is not configured! Please open 'src/main/resources/db.properties', replace 'YOUR_TIDB_PASSWORD_HERE' with your real TiDB password, and save the file.");
        }

        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?sslMode=%s&allowPublicKeyRetrieval=true&connectTimeout=10000",
            host, port, dbName, sslMode
        );

        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Run this main method to test your database connection.
     */
    public static void main(String[] args) {
        loadConfiguration();
        System.out.println("Testing connection to TiDB Cloud (" + host + ":" + port + "/" + dbName + ")...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(">>> SUCCESS! Connected to TiDB Cloud database successfully. <<<");
            }
        } catch (SQLException e) {
            System.err.println(">>> CONNECTION FAILED! <<<");
            System.err.println("Error message: " + e.getMessage());
        }
    }
}
