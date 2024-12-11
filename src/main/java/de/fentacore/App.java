package de.fentacore;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.dao.CustomerDAO;
import de.fentacore.dao.ReadingDAO;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;
import de.fentacore.model.Customer;
import de.fentacore.model.Reading;

import java.time.LocalDate;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        CustomerDAO customerDAO = new CustomerDAO();
        ReadingDAO readingDAO = new ReadingDAO();

        if(!DatabaseConfig.checkTablesExist()) {
            DatabaseConfig.createTables();
        }

        // Create a new customer
        Customer cust = new Customer();
        cust.setFirstName("John");
        cust.setLastName("Doe");
        cust.setBirthDate(LocalDate.of(1990, 1, 1));
        cust.setGender(ICustomer.Gender.M);
        ICustomer createdCustomer = customerDAO.create(cust);
        System.out.println("Created customer with ID: " + createdCustomer.getId());

        // Create a new reading for this customer
        Reading reading = new Reading();
        reading.setCustomer(createdCustomer);
        reading.setComment("Initial reading");
        reading.setDateOfReading(LocalDate.now());
        reading.setKindOfMeter(IReading.KindOfMeter.STROM);
        reading.setMeterCount(123.45);
        reading.setMeterId("MTR-001");
        reading.setSubstitute(false);

        IReading createdReading = readingDAO.create(reading);
        System.out.println("Created reading with ID: " + createdReading.getId());

        // Fetch and update customer
        ICustomer fetchedCustomer = customerDAO.findById(createdCustomer.getId());
        fetchedCustomer.setLastName("Smith");
        customerDAO.update(fetchedCustomer);
        System.out.println("Updated customer's last name to Smith.");

        // Find all customers
        customerDAO.findAll().forEach(c -> System.out.println("Customer: " + c.getId() + " - " + c.getLastName()));

        // Delete reading
        readingDAO.delete(createdReading.getId());
        System.out.println("Reading deleted.");

        // Delete customer
        customerDAO.delete(createdCustomer.getId());
        System.out.println("Customer deleted.");
    }
}
