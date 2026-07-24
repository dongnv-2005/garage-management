package com.garage.services;

import com.garage.models.Customer;
import com.garage.repository.CustomerRepository;
import java.util.List;

public class CustomerManager {
    private final CustomerRepository customerRepo;

    public CustomerManager(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    public boolean registerCustomer(String id, String name, String phone) {
        if (customerRepo.existsById(id)) {
            System.out.println("Mã khách hàng đã tồn tại!");
            return false;
        }
        Customer customer = new Customer(id, name, phone);
        customerRepo.save(customer);
        System.out.println("Đã thêm khách hàng thành công: " + name);
        return true;
    }

    public Customer findCustomer(String id) {
        return customerRepo.findById(id);
    }

    public List<Customer> findCustomerList() {
        return customerRepo.findAll();
    }

    public void listCustomers() {
        List<Customer> customers = customerRepo.findAll();
        if (customers.isEmpty()) {
            System.out.println("Chưa có khách hàng nào.");
            return;
        }
        System.out.println("\n--- DANH SÁCH KHÁCH HÀNG ---");
        customers.forEach(System.out::println);
    }
}