package de.fentacore.dao;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;
import de.fentacore.model.Customer;
import de.fentacore.model.Reading;
import org.junit.jupiter.api.Test;

import javax.xml.crypto.Data;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReadingDAOTest {

    @Test
    public void testCreateAndDeleteReading() {
        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();

        CustomerDAO customerDAO = new CustomerDAO();

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthDate(LocalDate.of(1980, 1, 1));
        customer.setGender(ICustomer.Gender.M);

        ReadingDAO readingDAO = new ReadingDAO();
        assertNotNull(readingDAO);

        Reading reading = new Reading();
        reading.setCustomer(customerDAO.create(customer));
        reading.setDateOfReading(LocalDate.of(2020, 1, 1));
        reading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        reading.setComment("Test");
        reading.setMeterCount(12345d);
        reading.setMeterId("test");

        assertNotNull(readingDAO.create(reading).getId());

        Reading newReading = (Reading) readingDAO.findById(reading.getId());

        assertNotNull(newReading);

        newReading.setComment("Test2");

        readingDAO.update(newReading);

        Reading updatedReading = (Reading) readingDAO.findById(newReading.getId());

        assertNotNull(updatedReading);
        assertEquals("Test2", updatedReading.getComment());

        assertEquals(1, readingDAO.findAll().size());

        readingDAO.delete(newReading.getId());
        assertNull(readingDAO.findById(newReading.getId()));

        assertEquals(0, readingDAO.findAll().size());

        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();
    }
}
