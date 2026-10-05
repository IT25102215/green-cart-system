package com.greencart.designpattern.member3order.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Concrete Subject for Order status changes. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusSubject {
    private final List<OrderStatusObserver> observers;

    public void notifyObservers(OrderStatusEvent event) {
        for (OrderStatusObserver observer : observers) {
            try {
                observer.update(event);
            } catch (RuntimeException ex) {
                // Status persistence must remain successful even when a notification channel fails.
                log.warn("Order observer {} failed: {}",
                        observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }
}
