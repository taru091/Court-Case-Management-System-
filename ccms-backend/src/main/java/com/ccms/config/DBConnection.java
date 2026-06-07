package com.ccms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String DB_URL = System.getenv().getOrDefault(
            "CCMS_DB_URL",
            "jdbc:mysql://localhost:3306/ccms_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    );
    private static final String DB_USER = System.getenv().getOrDefault("CCMS_DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("CCMS_DB_PASSWORD", "root");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("Unable to load MySQL JDBC driver.", exception);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            return openInitializedConnection();
        } catch (SQLException exception) {
            if (!isMissingDatabaseError(exception)) {
                throw exception;
            }

            createDatabaseIfMissing();
            return openInitializedConnection();
        }
    }

    private static Connection openInitializedConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        SchemaBootstrap.initialize(connection);
        return connection;
    }

    private static boolean isMissingDatabaseError(SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (current.getErrorCode() == 1049 ||
                    (message != null && message.toLowerCase().contains("unknown database"))) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }

    private static void createDatabaseIfMissing() throws SQLException {
        String serverUrl = buildServerUrl(DB_URL);
        String databaseName = extractDatabaseName(DB_URL);

        if (serverUrl == null || databaseName == null) {
            throw new SQLException("Unable to derive the MySQL server URL from CCMS_DB_URL: " + DB_URL);
        }

        try (Connection connection = DriverManager.getConnection(serverUrl, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + databaseName.replace("`", "``") + "`");
        }
    }

    private static String buildServerUrl(String jdbcUrl) {
        String prefix = "jdbc:mysql://";
        if (!jdbcUrl.startsWith(prefix)) {
            return null;
        }

        int pathStart = jdbcUrl.indexOf('/', prefix.length());
        if (pathStart < 0) {
            return null;
        }

        int queryStart = jdbcUrl.indexOf('?', pathStart);
        String querySuffix = queryStart >= 0 ? jdbcUrl.substring(queryStart) : "";
        return jdbcUrl.substring(0, pathStart) + "/" + querySuffix;
    }

    private static String extractDatabaseName(String jdbcUrl) {
        String prefix = "jdbc:mysql://";
        if (!jdbcUrl.startsWith(prefix)) {
            return null;
        }

        int pathStart = jdbcUrl.indexOf('/', prefix.length());
        if (pathStart < 0 || pathStart == jdbcUrl.length() - 1) {
            return null;
        }

        int queryStart = jdbcUrl.indexOf('?', pathStart);
        String databaseName = queryStart >= 0
                ? jdbcUrl.substring(pathStart + 1, queryStart)
                : jdbcUrl.substring(pathStart + 1);

        return databaseName.isBlank() ? null : databaseName;
    }
}
