package de.fentacore;

import de.fentacore.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        App app = new App();
        System.out.println(app.greet("World"));

        // Test database connection
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("Successfully connected to the database!");
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }
    }
}
