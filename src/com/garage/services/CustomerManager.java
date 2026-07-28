package com.garage.services;

import com.garage.models.Customer;
import com.garage.repository.CustomerRepository;

import java.sql.SQLException;
import java.util.List;

public class CustomerManager {
    private final CustomerRepository repository = new CustomerRepository();

    public List<Customer> getAllCustomers() {
        return repository.findAll();
    }

    public List<Customer> searchCustomers(String keyword) {
        return repository.search(keyword);
    }

    public void addCustomer(String name, String phone) throws SQLException {
        String id = repository.generateNextId();
        repository.save(new Customer(id, name, phone));
    }

    public void updateCustomer(String id, String name, String phone) throws SQLException {
        repository.update(new Customer(id, name, phone));
    }
}