package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";

    private static String activeUser = "postgres";
    private static String activePassword = "1234";

    public static void setCredentials(String user, String password) {
        activeUser = user;
        activePassword = password;
        System.out.println("[System] Switched database user to: " + activeUser);
    }

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(URL, activeUser, activePassword);
        } catch (SQLException e) {
            System.err.println("[Database] Connection failed: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[Database] JDBC Driver not found.");
        }
        return conn;
    }
}