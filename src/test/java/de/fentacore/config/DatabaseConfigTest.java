package de.fentacore.config;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @Test
    public void testGetConnection() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAndDeleteTables() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            if(DatabaseConfig.checkTablesExist()) {
                DatabaseConfig.deleteTables();
            }
            DatabaseConfig.createTables();
            assertTrue(DatabaseConfig.checkTablesExist());
            DatabaseConfig.deleteTables();
            assertFalse(DatabaseConfig.checkTablesExist());

            DatabaseConfig.createTables();
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }
}
