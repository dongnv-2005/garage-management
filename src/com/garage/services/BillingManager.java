package com.garage.services;

import com.garage.enums.RepairStatus;
import com.garage.models.Invoice;
import com.garage.models.Part;
import com.garage.repository.InvoiceRepository;
import com.garage.repository.PartRepository;
import com.garage.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.List;

public class BillingManager {
    private final InvoiceRepository invoiceRepository = new InvoiceRepository();
    private final PartRepository partRepository = new PartRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> searchInvoices(String keyword) {
        return invoiceRepository.search(keyword);
    }

    public void createInvoice(String licensePlate, String serviceType, String selectedPartRaw, int partQty, String notes, double totalAmount, String currentUsername) throws SQLException {
        String invId = "INV-" + new java.text.SimpleDateFormat("yyMMddHHmmssSSS").format(new java.util.Date());
        String partInfoStr = "---";

        if ("Thay thế phụ tùng kho".equals(serviceType)) {
            if (selectedPartRaw != null && !selectedPartRaw.isEmpty()) {
                String partId = selectedPartRaw.split(" - ")[0];
                Part part = partRepository.findById(partId);
                if (part != null) {
                    if (part.getStockQuantity() < partQty) {
                        throw new IllegalArgumentException("Số lượng tồn kho không đủ! Hiện còn: " + part.getStockQuantity());
                    }
                    partInfoStr = part.getName() + " (SL: " + partQty + ")";
                    partRepository.reduceStock(partId, partQty);
                    partRepository.logPartTransaction(partId, part.getName(), partQty, licensePlate, currentUsername);
                }
            }
        }

        Invoice invoice = new Invoice(invId, licensePlate, null, serviceType, partInfoStr, notes.isEmpty() ? "---" : notes, currentUsername, totalAmount, null);
        invoiceRepository.save(invoice);
        vehicleRepository.updateStatus(licensePlate, RepairStatus.COMPLETED);
    }

    public void deleteInvoice(String invoiceId) throws SQLException {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice != null) {
            String partInfo = invoice.getPartInfo();
            if (partInfo != null && !partInfo.equals("---") && partInfo.contains("(SL:")) {
                try {
                    String partName = partInfo.substring(0, partInfo.lastIndexOf("(SL:")).trim();
                    String qtyStr = partInfo.substring(partInfo.lastIndexOf("(SL:") + 4, partInfo.lastIndexOf(")")).trim();
                    int qty = Integer.parseInt(qtyStr);

                    List<Part> allParts = partRepository.findAll();
                    for (Part p : allParts) {
                        if (p.getName().equalsIgnoreCase(partName)) {
                            partRepository.addStock(p.getId(), qty, p.getExportPrice());
                            partRepository.logPartTransaction(p.getId(), p.getName(), qty, "HOÀN KHO DO XÓA HĐ", invoice.getCreatedBy());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            invoiceRepository.delete(invoiceId);
        }
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public List<Object[]> getPartUsageLogs() {
        return partRepository.findUsageLogs();
    }

    public void importPart(String name, double importPrice, double exportPrice, int qty, String currentUsername) throws SQLException {
        Part existing = partRepository.findByNameAndImportPrice(name, importPrice);
        if (existing != null) {
            partRepository.addStock(existing.getId(), qty, exportPrice);
            partRepository.logPartTransaction(existing.getId(), name, qty, "NHẬP KHO", currentUsername);
        } else {
            String newId = partRepository.generateNextId();
            partRepository.save(new Part(newId, name, importPrice, exportPrice, qty));
            partRepository.logPartTransaction(newId, name, qty, "NHẬP KHO", currentUsername);
        }
    }
}