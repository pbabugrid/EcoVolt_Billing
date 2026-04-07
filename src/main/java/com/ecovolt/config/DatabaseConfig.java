package com.ecovolt.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place for DB connection details.
 * Change the constants here or pass a Connection directly in tests.
 */
public class DatabaseConfig {

    public static final String URL = "jdbc:mysql://localhost:3306/ecovolt_billing";
    public static final String USER = "ecovolt_user";
    public static final String PASSWORD = "neeraj";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private DatabaseConfig() {
    } // utility class
}
