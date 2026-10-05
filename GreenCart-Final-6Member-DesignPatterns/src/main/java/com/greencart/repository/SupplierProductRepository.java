package com.greencart.repository;

import com.greencart.entity.Product;
import com.greencart.entity.Supplier;
import com.greencart.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {
    List<SupplierProduct> findBySupplier_ActiveTrueAndActiveTrueOrderByAssignedAtDesc();
    List<SupplierProduct> findBySupplierOrderByAssignedAtDesc(Supplier supplier);
    Optional<SupplierProduct> findBySupplierAndProduct(Supplier supplier, Product product);
}
