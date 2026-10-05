package com.greencart.designpattern.member1product.observer;

/** Observer contract from the lecture: observers react when subject state changes. */
public interface InventoryObserver {
    void update(InventoryStockEvent event);
}
