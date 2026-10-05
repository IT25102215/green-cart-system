package com.greencart.designpattern.member5supplier.factory;

import com.greencart.entity.Product;
import com.greencart.entity.PurchaseOrder;
import com.greencart.entity.PurchaseOrderStatus;
import com.greencart.entity.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Concrete creator for purchase orders that are still being prepared. */
@Component
public class DraftPurchaseOrderCreator implements PurchaseOrderCreator {
    @Override
    public PurchaseOrderStatus supportedStatus() {
        return PurchaseOrderStatus.DRAFT;
    }

    @Override
    public PurchaseOrder create(Supplier supplier, Product product, Integer quantity,
                                BigDecimal unitPrice, LocalDate expectedDeliveryDate) {
        return PurchaseOrder.builder()
                .supplier(supplier)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .expectedDeliveryDate(expectedDeliveryDate)
                .status(PurchaseOrderStatus.DRAFT)
                .receivedQuantity(0)
                .build();
    }
}
