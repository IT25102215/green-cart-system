package com.greencart.service;

import com.greencart.entity.Product;
import com.greencart.entity.Supplier;
import com.greencart.entity.SupplierProduct;
import com.greencart.repository.SupplierProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final SupplierProductRepository repository;

    public List<SupplierProduct> all() {
        return repository.findBySupplier_ActiveTrueAndActiveTrueOrderByAssignedAtDesc();
    }

    public SupplierProduct get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier-product assignment not found"));
    }

    public SupplierProduct assign(Supplier supplier, Product product, BigDecimal supplierPrice) {
        SupplierProduct assignment = repository.findBySupplierAndProduct(supplier, product)
                .orElseGet(SupplierProduct::new);

        assignment.setSupplier(supplier);
        assignment.setProduct(product);
        assignment.setSupplierPrice(supplierPrice);
        assignment.setActive(true);
        return repository.save(assignment);
    }

    public void delete(Long id) {
        SupplierProduct assignment = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier-product assignment not found"));
        repository.delete(assignment);
    }
}
