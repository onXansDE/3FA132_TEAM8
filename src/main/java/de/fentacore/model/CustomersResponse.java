package de.fentacore.model;

import de.fentacore.interfaces.ICustomer;
import java.util.List;

public class CustomersResponse {
    private List<ICustomer> customers;

    public CustomersResponse() {
    }

    public CustomersResponse(List<ICustomer> customers) {
        this.customers = customers;
    }

    public List<ICustomer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<ICustomer> customers) {
        this.customers = customers;
    }
} 