package com.greencart.service;

import com.greencart.designpattern.member1product.observer.InventoryStockEvent;
import com.greencart.designpattern.member1product.observer.InventorySubject;
import com.greencart.dto.SupplierPerformanceDto;
import com.greencart.entity.*;
import com.greencart.repository.ProductRepository;
import com.greencart.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** Purchase-order business logic - IT25102587 (Gunarathne B.K.P.R.). */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository repository;
    private final ProductRepository productRepository;
    private final InventorySubject inventorySubject;

    public List<PurchaseOrder> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public PurchaseOrder get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
    }

    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getReceivedQuantity() == null) {
            purchaseOrder.setReceivedQuantity(0);
        }
        return repository.save(purchaseOrder);
    }

    @Transactional
    public PurchaseOrder updateDeliveryStatus(Long id,
                                              PurchaseOrderStatus newStatus,
                                              Integer receivedQuantity,
                                              String deliveryNote) {
        PurchaseOrder purchaseOrder = get(id);
        PurchaseOrderStatus oldStatus = purchaseOrder.getStatus();

        if (oldStatus == PurchaseOrderStatus.RECEIVED && newStatus != PurchaseOrderStatus.RECEIVED) {
            throw new IllegalArgumentException("A received purchase order cannot be moved back to another status");
        }
        if (oldStatus == PurchaseOrderStatus.CANCELLED && newStatus != PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("A cancelled purchase order cannot be reopened");
        }

        purchaseOrder.setDeliveryNote(
                deliveryNote == null || deliveryNote.isBlank() ? null : deliveryNote.trim()
        );

        if (newStatus == PurchaseOrderStatus.RECEIVED) {
            int received = receivedQuantity == null ? purchaseOrder.getQuantity() : receivedQuantity;
            if (received < 1) {
                throw new IllegalArgumentException("Received quantity must be at least 1");
            }
            if (received > purchaseOrder.getQuantity()) {
                throw new IllegalArgumentException("Received quantity cannot exceed ordered quantity");
            }

            if (oldStatus != PurchaseOrderStatus.RECEIVED) {
                Product product = purchaseOrder.getProduct();
                int currentStock = product.getStock() == null ? 0 : product.getStock();
                int newStock = currentStock + received;
                product.setStock(newStock);
                productRepository.save(product);
                inventorySubject.notifyObservers(new InventoryStockEvent(
                        product, currentStock, newStock, "SYSTEM"
                ));

                purchaseOrder.setReceivedAt(LocalDateTime.now());
            }

            purchaseOrder.setReceivedQuantity(received);
        }

        purchaseOrder.setStatus(newStatus);
        return repository.save(purchaseOrder);
    }

    public SupplierPerformanceDto performanceFor(Supplier supplier) {
        List<PurchaseOrder> orders = repository.findBySupplierOrderByCreatedAtDesc(supplier);
        long total = orders.size();
        long received = orders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.RECEIVED)
                .count();
        long cancelled = orders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.CANCELLED)
                .count();
        long onTime = orders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.RECEIVED)
                .filter(PurchaseOrder::isOnTime)
                .count();

        BigDecimal completionRate = percentage(received, total);
        BigDecimal onTimeRate = percentage(onTime, received);

        return new SupplierPerformanceDto(
                supplier,
                total,
                received,
                cancelled,
                onTime,
                completionRate,
                onTimeRate
        );
    }

    private BigDecimal percentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }
}
