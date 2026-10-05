package com.greencart.designpattern.member1product.observer;

import com.greencart.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Concrete Observer: creates an alert when stock becomes low or reaches zero. */
@Component
@RequiredArgsConstructor
public class LowStockAlertObserver implements InventoryObserver {
    private final AuditService auditService;

    @Value("${app.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Override
    public void update(InventoryStockEvent event) {
        if (event.product() == null || event.product().getId() == null || event.newStock() == null) return;

        int newStock = event.newStock();
        boolean crossedIntoLowStock = newStock <= lowStockThreshold
                && (event.oldStock() == null || event.oldStock() > lowStockThreshold || !event.oldStock().equals(newStock));
        if (!crossedIntoLowStock) return;

        String level = newStock <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
        auditService.log(
                "INVENTORY",
                "OBSERVER_" + level,
                "Product",
                event.product().getId(),
                level.replace('_', ' ') + " alert for '" + event.product().getName()
                        + "' (stock: " + newStock + ", threshold: " + lowStockThreshold + ")",
                event.actorEmail() == null || event.actorEmail().isBlank() ? "SYSTEM" : event.actorEmail()
        );
    }
}
