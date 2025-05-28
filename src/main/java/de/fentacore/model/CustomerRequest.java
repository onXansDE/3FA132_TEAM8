package de.fentacore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerRequest {
    @JsonProperty("customer")
    private Customer customer;

    public CustomerRequest() {
    }

    public CustomerRequest(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
} 