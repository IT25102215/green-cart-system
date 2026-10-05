package com.greencart.designpattern.member3order.observer;

import com.greencart.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Concrete Observer: persists a trace of order lifecycle notifications. */
@Component
@RequiredArgsConstructor
public class OrderAuditObserver implements OrderStatusObserver {
    private final AuditService auditService;

    @Override
    public void update(OrderStatusEvent event) {
        if (event.orderId() == null || event.newStatus() == null) return;
        auditService.logChange(
                "ORDER",
                "OBSERVER_STATUS_EVENT",
                "Order",
                event.orderId(),
                event.oldStatus() == null ? "CREATED" : event.oldStatus().name(),
                event.newStatus().name(),
                event.note() == null || event.note().isBlank()
                        ? "Order lifecycle observer notified"
                        : event.note(),
                event.actorEmail() == null || event.actorEmail().isBlank() ? "SYSTEM" : event.actorEmail()
        );
    }
}
