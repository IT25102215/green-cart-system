package com.greencart.designpattern.member1product.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Concrete Subject for Product stock changes.
 * Spring injects every InventoryObserver implementation into the list.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySubject {
    private final List<InventoryObserver> observers;

    public void notifyObservers(InventoryStockEvent event) {
        for (InventoryObserver observer : observers) {
            try {
                observer.update(event);
            } catch (RuntimeException ex) {
                // An alert/observer must never make a successful stock save fail.
                log.warn("Inventory observer {} failed: {}",
                        observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }
}
