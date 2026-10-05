package com.greencart.service;

import com.greencart.entity.Supplier;
import com.greencart.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    public List<Supplier> all() { return repository.findByActiveTrueOrderByNameAsc(); }

    public Supplier get(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    public Optional<Supplier> findByName(String name) {
        return name == null ? Optional.empty() : repository.findByNameIgnoreCaseAndActiveTrue(name.trim());
    }

    public Optional<Supplier> findByEmail(String email) {
        return email == null ? Optional.empty() : repository.findByEmailIgnoreCaseAndActiveTrue(email.trim());
    }

    public Supplier save(Supplier supplier) {
        supplier.setActive(true);
        return repository.save(supplier);
    }

    /** Soft delete: preserve purchase-order history and relationships. */
    @Transactional
    public void delete(Long id) {
        Supplier supplier = get(id);
        supplier.setActive(false);
        repository.save(supplier);
    }

    public long count() { return repository.countByActiveTrue(); }
}
