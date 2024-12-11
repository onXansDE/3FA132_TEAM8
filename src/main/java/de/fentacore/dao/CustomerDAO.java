package de.fentacore.dao;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerDAO implements ICustomerDAO {

    @Override
    public ICustomer create(ICustomer customer) {
        String sql = "INSERT INTO customers (id, first_name, last_name, birth_date, gender) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            UUID newId = (customer.getId() == null) ? UUID.randomUUID() : customer.getId();
            stmt.setString(1, newId.toString());
            stmt.setString(2, customer.getFirstName());
            stmt.setString(3, customer.getLastName());
            stmt.setDate(4, customer.getBirthDate() != null ? Date.valueOf(customer.getBirthDate()) : null);
            stmt.setString(5, customer.getGender() != null ? customer.getGender().name() : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                customer.setId(newId);
                return customer;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ICustomer findById(UUID id) {
        String sql = "SELECT id, first_name, last_name, birth_date, gender FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Customer c = new Customer();
                    c.setId(UUID.fromString(rs.getString("id")));
                    c.setFirstName(rs.getString("first_name"));
                    c.setLastName(rs.getString("last_name"));
                    Date bd = rs.getDate("birth_date");
                    c.setBirthDate(bd != null ? bd.toLocalDate() : null);
                    String gender = rs.getString("gender");
                    if (gender != null) {
                        c.setGender(ICustomer.Gender.valueOf(gender));
                    }
                    return c;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ICustomer> findAll() {
        List<ICustomer> customers = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, birth_date, gender FROM customers";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Customer c = new Customer();
                c.setId(UUID.fromString(rs.getString("id")));
                c.setFirstName(rs.getString("first_name"));
                c.setLastName(rs.getString("last_name"));
                Date bd = rs.getDate("birth_date");
                c.setBirthDate(bd != null ? bd.toLocalDate() : null);
                String gender = rs.getString("gender");
                if (gender != null) {
                    c.setGender(ICustomer.Gender.valueOf(gender));
                }
                customers.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    @Override
    public boolean update(ICustomer customer) {
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, birth_date = ?, gender = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setDate(3, customer.getBirthDate() != null ? Date.valueOf(customer.getBirthDate()) : null);
            stmt.setString(4, customer.getGender() != null ? customer.getGender().name() : null);
            stmt.setString(5, customer.getId().toString());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
