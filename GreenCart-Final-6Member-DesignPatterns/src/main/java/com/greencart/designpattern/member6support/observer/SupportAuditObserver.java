package com.greencart.designpattern.member6support.observer;

import com.greencart.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Concrete Observer: writes each new support request to the common audit page. */
@Component
@RequiredArgsConstructor
public class SupportAuditObserver implements SupportRequestObserver {
    private final AuditService auditService;

    @Override
    public void update(SupportRequestEvent event) {
        auditService.log(
                "SUPPORT",
                "OBSERVER_NEW_" + event.requestType().toUpperCase(),
                event.requestType(),
                event.entityId(),
                "Observer received new " + event.requestType().toLowerCase() + ": " + event.subject(),
                event.customerEmail() == null || event.customerEmail().isBlank() ? "SYSTEM" : event.customerEmail()
        );
    }
}
