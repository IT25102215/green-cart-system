package com.greencart.designpattern.member5supplier.factory;

import com.greencart.entity.Product;
import com.greencart.entity.PurchaseOrder;
import com.greencart.entity.PurchaseOrderStatus;
import com.greencart.entity.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Creator contract used by the Supplier module Factory. */
public interface PurchaseOrderCreator {
    PurchaseOrderStatus supportedStatus();

    PurchaseOrder create(Supplier supplier,
                         Product product,
                         Integer quantity,
                         BigDecimal unitPrice,
                         LocalDate expectedDeliveryDate);
}
