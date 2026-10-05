package com.greencart.designpattern.member3order.observer;

import com.greencart.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Concrete Observer: notifies the customer when their order state changes. */
@Component
@RequiredArgsConstructor
public class CustomerOrderNotificationObserver implements OrderStatusObserver {
    private final MailService mailService;

    @Override
    public void update(OrderStatusEvent event) {
        if (event.customerEmail() == null || event.customerEmail().isBlank() || event.newStatus() == null) return;
        mailService.sendOrderStatusUpdate(
                event.customerEmail(),
                event.customerName(),
                event.orderId(),
                event.newStatus().name(),
                event.note()
        );
    }
}
