package com.garage.repository;

import com.garage.models.Customer;
import java.util.*;

public class CustomerRepository {
    private final Map<String, Customer> customers = new HashMap<>();

    public void save(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    public Customer findById(String id) {
        return customers.get(id);
    }

    public boolean existsById(String id) {
        return customers.containsKey(id);
    }

    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }
}