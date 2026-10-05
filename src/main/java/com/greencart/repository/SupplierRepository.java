package com.greencart.repository;

import com.greencart.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier,Long> {
    Optional<Supplier> findByNameIgnoreCaseAndActiveTrue(String name);
    Optional<Supplier> findByEmailIgnoreCaseAndActiveTrue(String email);
    Optional<Supplier> findByIdAndActiveTrue(Long id);
    List<Supplier> findByActiveTrueOrderByNameAsc();
    long countByActiveTrue();
}
