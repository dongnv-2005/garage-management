package com.garage.repository;

import com.garage.models.Invoice;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {
    private final List<Invoice> invoices = new ArrayList<>();

    public void save(Invoice invoice) {
        invoices.add(invoice);
    }

    public List<Invoice> findAll() {
        return new ArrayList<>(invoices);
    }
}