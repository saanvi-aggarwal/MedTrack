
package com.medtrack.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mariadb://localhost:3306/medtrack";

    private static volatile String url =
            getConfig("medtrack.db.url", "MEDTRACK_DB_URL", DEFAULT_URL);

    private static final String user =
            getConfig("medtrack.db.user", "MEDTRACK_DB_USER", "root");

    private static final String password =
            getConfig("medtrack.db.password", "MEDTRACK_DB_PASSWORD", null);

    private static String getConfig(
            String property,
            String environmentVariable,
            String defaultValue) {

        String value = System.getProperty(property);

        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    public static void setCustomUrl(String customUrl) {

        if (customUrl == null || customUrl.isBlank()
                || !customUrl.startsWith("jdbc:mariadb://")) {
            throw new IllegalArgumentException(
                    "A valid MariaDB JDBC URL is required."
            );
        }

        url = customUrl;
    }

    public static void resetUrl() {
        url = DEFAULT_URL;
    }

    public static Connection getConnection() throws SQLException {

        if (password == null || password.isBlank()) {
            throw new SQLException(
                    "Database password is not configured. "
                    + "Set MEDTRACK_DB_PASSWORD or "
                    + "medtrack.db.password."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }
}
