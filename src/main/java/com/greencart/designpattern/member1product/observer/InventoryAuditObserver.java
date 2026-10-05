package com.greencart.designpattern.member1product.observer;

import com.greencart.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Concrete Observer: records stock state changes in the system audit log. */
@Component
@RequiredArgsConstructor
public class InventoryAuditObserver implements InventoryObserver {
    private final AuditService auditService;

    @Override
    public void update(InventoryStockEvent event) {
        if (event.product() == null || event.product().getId() == null) return;
        if (event.oldStock() != null && event.oldStock().equals(event.newStock())) return;

        auditService.logChange(
                "INVENTORY",
                "OBSERVER_STOCK_CHANGE",
                "Product",
                event.product().getId(),
                event.oldStock() == null ? "NEW" : String.valueOf(event.oldStock()),
                String.valueOf(event.newStock()),
                "Observer detected stock state change for '" + event.product().getName() + "'",
                safeActor(event.actorEmail())
        );
    }

    private String safeActor(String actor) {
        return actor == null || actor.isBlank() ? "SYSTEM" : actor;
    }
}
