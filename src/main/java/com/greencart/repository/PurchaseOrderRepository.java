package com.greencart.repository;

import com.greencart.entity.PurchaseOrder;
import com.greencart.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findBySupplierOrderByCreatedAtDesc(Supplier supplier);
}
