package de.fentacore.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseConfig {
    private static Properties props;

    static {
        try {
            props = new Properties();
            try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
                if (input == null) {
                    throw new RuntimeException("database.properties not found on classpath");
                }
                props.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = props.getProperty("db.url");
        String username = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Checks if the required tables ('customers' and 'readings') exist in the database.
     *
     * @return true if both tables exist, false otherwise.
     */
    public static boolean checkTablesExist() {
        String[] requiredTables = {"customers", "readings"};
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            for (String tableName : requiredTables) {
                try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                    if (!rs.next()) {
                        // Table not found
                        return false;
                    }
                }
            }
            return true; // All required tables found

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates the 'customers' and 'readings' tables if they do not exist.
     * <p>
     * This method will execute the DDL statements to create the required tables.
     * Adjust the DDL statements according to your schema and database dialect.
     */
    public static void createTables() {
        String createCustomersTable =
                "CREATE TABLE IF NOT EXISTS customers (" +
                        "  id VARCHAR(36) PRIMARY KEY," +
                        "  first_name VARCHAR(100) NOT NULL," +
                        "  last_name VARCHAR(100) NOT NULL," +
                        "  birth_date DATE," +
                        "  gender VARCHAR(1) CHECK (gender IN ('D','M','U','W'))" +
                        ");";

        String createReadingsTable =
                "CREATE TABLE IF NOT EXISTS readings (" +
                        "  id VARCHAR(36) PRIMARY KEY," +
                        "  customer_id VARCHAR(36) NOT NULL," +
                        "  comment VARCHAR(255)," +
                        "  date_of_reading DATE," +
                        "  kind_of_meter VARCHAR(20) CHECK (kind_of_meter IN ('HEIZUNG','STROM','UNBEKANNT','WASSER'))," +
                        "  meter_count DOUBLE," +
                        "  meter_id VARCHAR(50)," +
                        "  substitute BOOLEAN," +
                        "  FOREIGN KEY (customer_id) REFERENCES customers(id)" +
                        ");";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createCustomersTable);
                stmt.execute(createReadingsTable);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                throw new RuntimeException("Failed to create tables", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection issue when creating tables", e);
        }
    }

    public static void deleteTables() {
        String deleteReadingsTable = "DROP TABLE IF EXISTS readings;";
        String deleteCustomersTable = "DROP TABLE IF EXISTS customers;";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(deleteReadingsTable);
                stmt.execute(deleteCustomersTable);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                throw new RuntimeException("Failed to delete tables", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection issue when deleting tables", e);
        }
    }
}
