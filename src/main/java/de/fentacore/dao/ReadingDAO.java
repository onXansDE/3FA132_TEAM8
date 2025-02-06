package de.fentacore.dao;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.interfaces.IReading;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.model.Reading;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReadingDAO implements IReadingDAO {

    private final ICustomerDAO customerDAO = new CustomerDAO();

    @Override
    public IReading create(IReading reading) {
        String sql = "INSERT INTO readings (id, customer_id, comment, date_of_reading, kind_of_meter, meter_count, meter_id, substitute) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            UUID newId = (reading.getId() == null) ? UUID.randomUUID() : reading.getId();
            stmt.setString(1, newId.toString());
            stmt.setString(2, reading.getCustomer().getId().toString());
            stmt.setString(3, reading.getComment());
            stmt.setDate(4, reading.getDateOfReading() != null ? Date.valueOf(reading.getDateOfReading()) : null);
            stmt.setString(5, reading.getKindOfMeter() != null ? reading.getKindOfMeter().name() : null);
            stmt.setDouble(6, reading.getMeterCount() != null ? reading.getMeterCount() : 0.0);
            stmt.setString(7, reading.getMeterId());
            stmt.setBoolean(8, reading.getSubstitute() != null ? reading.getSubstitute() : false);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                reading.setId(newId);
                return reading;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public IReading findById(UUID id) {
        String sql = "SELECT id, customer_id, comment, date_of_reading, kind_of_meter, meter_count, meter_id, substitute FROM readings WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReading(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<IReading> findAll() {
        List<IReading> readings = new ArrayList<>();
        String sql = "SELECT id, customer_id, comment, date_of_reading, kind_of_meter, meter_count, meter_id, substitute FROM readings";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Reading r = mapResultSetToReading(rs);
                readings.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return readings;
    }

    @Override
    public boolean update(IReading reading) {
        String sql = "UPDATE readings SET customer_id = ?, comment = ?, date_of_reading = ?, kind_of_meter = ?, meter_count = ?, meter_id = ?, substitute = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reading.getCustomer().getId().toString());
            stmt.setString(2, reading.getComment());
            stmt.setDate(3, reading.getDateOfReading() != null ? Date.valueOf(reading.getDateOfReading()) : null);
            stmt.setString(4, reading.getKindOfMeter() != null ? reading.getKindOfMeter().name() : null);
            stmt.setDouble(5, reading.getMeterCount() != null ? reading.getMeterCount() : 0.0);
            stmt.setString(6, reading.getMeterId());
            stmt.setBoolean(7, reading.getSubstitute() != null ? reading.getSubstitute() : false);
            stmt.setString(8, reading.getId().toString());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM readings WHERE id = ?";
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

    @Override
    public List<IReading> findByCustomerId(UUID customerId) {
        List<IReading> readings = new ArrayList<>();
        String sql = "SELECT id, customer_id, comment, date_of_reading, kind_of_meter, meter_count, meter_id, substitute FROM readings WHERE customer_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reading r = mapResultSetToReading(rs);
                    readings.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return readings;
    }

    private Reading mapResultSetToReading(ResultSet rs) throws SQLException {
        Reading r = new Reading();
        r.setId(UUID.fromString(rs.getString("id")));
        UUID customId = UUID.fromString(rs.getString("customer_id"));
        ICustomer c = customerDAO.findById(customId);
        r.setCustomer(c);
        r.setComment(rs.getString("comment"));
        Date dor = rs.getDate("date_of_reading");
        r.setDateOfReading(dor != null ? dor.toLocalDate() : null);
        String kom = rs.getString("kind_of_meter");
        if (kom != null) {
            r.setKindOfMeter(IReading.KindOfMeter.valueOf(kom));
        }
        double mc = rs.getDouble("meter_count");
        r.setMeterCount(rs.wasNull() ? null : mc);
        r.setMeterId(rs.getString("meter_id"));
        boolean sub = rs.getBoolean("substitute");
        r.setSubstitute(rs.wasNull() ? null : sub);

        return r;
    }
}
