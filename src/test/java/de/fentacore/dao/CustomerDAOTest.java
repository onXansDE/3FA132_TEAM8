package de.fentacore.dao;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.model.Customer;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerDAOTest {


    @Test
    public void testCreateAndDeleteCustomer() {
        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();

        CustomerDAO conn = new CustomerDAO();
        assertNotNull(conn);

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthDate(LocalDate.of(1980, 1, 1));
        customer.setGender(ICustomer.Gender.M);

        customer = (Customer) conn.create(customer);

        assertNotNull(customer.getId());

        customer = (Customer) conn.findById(customer.getId());

        assertNotNull(customer);

        customer.setFirstName("Jane");

        conn.update(customer);

        // check if name got updated
        Customer updatedCustomer = (Customer) conn.findById(customer.getId());
        assertNotNull(updatedCustomer);
        assertEquals("Jane", updatedCustomer.getFirstName());

        conn.delete(customer.getId());

        // check if customer got deleted
        Customer deletedCustomer = (Customer) conn.findById(customer.getId());
        assertNull(deletedCustomer);

        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();
    }

    @Test
    public void testFindAllCustomers() {
        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();

        CustomerDAO conn = new CustomerDAO();

        assertEquals(0, conn.findAll().size());

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthDate(LocalDate.of(1980, 1, 1));
        customer.setGender(ICustomer.Gender.M);

        conn.create(customer);

        assertNotNull(customer.getId());

        assertEquals(1, conn.findAll().size());

        Customer customer2 = new Customer();
        customer2.setFirstName("Jane");
        customer2.setLastName("Doe");
        customer2.setBirthDate(LocalDate.of(1980, 1, 1));
        customer2.setGender(ICustomer.Gender.W);

        conn.create(customer2);

        assertEquals(2, conn.findAll().size());

        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();
    }
}
