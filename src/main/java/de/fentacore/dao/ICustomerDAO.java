package de.fentacore.dao;

import de.fentacore.interfaces.ICustomer;
import java.util.List;
import java.util.UUID;

public interface ICustomerDAO {
    ICustomer create(ICustomer customer);
    ICustomer findById(UUID id);
    List<ICustomer> findAll();
    boolean update(ICustomer customer);
    boolean delete(UUID id);
}
