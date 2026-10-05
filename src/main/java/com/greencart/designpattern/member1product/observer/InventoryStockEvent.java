package com.greencart.designpattern.member1product.observer;

import com.greencart.entity.Product;

/** Data sent by the inventory Subject to every registered Observer. */
public record InventoryStockEvent(
        Product product,
        Integer oldStock,
        Integer newStock,
        String actorEmail
) {
    public boolean isNewProduct() {
        return oldStock == null;
    }
}
