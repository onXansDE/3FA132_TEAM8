package de.fentacore.dao;

import de.fentacore.interfaces.IReading;
import java.util.List;
import java.util.UUID;

public interface IReadingDAO {
    IReading create(IReading reading);
    IReading findById(UUID id);
    List<IReading> findAll();
    boolean update(IReading reading);
    boolean delete(UUID id);
    List<IReading> findByCustomerId(UUID customerId);
}
