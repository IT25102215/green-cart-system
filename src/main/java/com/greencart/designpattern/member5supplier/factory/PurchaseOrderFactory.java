package com.greencart.designpattern.member5supplier.factory;

import com.greencart.entity.Product;
import com.greencart.entity.PurchaseOrder;
import com.greencart.entity.PurchaseOrderStatus;
import com.greencart.entity.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Member 5 Factory Pattern.
 * The client asks one Factory for a PurchaseOrder. The factory chooses the
 * appropriate concrete creator, hiding creation details from the controller.
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderFactory {
    private final List<PurchaseOrderCreator> creators;

    public PurchaseOrder create(Supplier supplier,
                                Product product,
                                Integer quantity,
                                BigDecimal unitPrice,
                                LocalDate expectedDeliveryDate,
                                PurchaseOrderStatus requestedStatus) {
        if (supplier == null) throw new IllegalArgumentException("Supplier is required");
        if (product == null) throw new IllegalArgumentException("Product is required");
        if (quantity == null || quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price must be zero or greater");

        PurchaseOrderStatus status = requestedStatus == null ? PurchaseOrderStatus.DRAFT : requestedStatus;
        if (status == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalArgumentException(
                    "Create the purchase order first, then record the supplier delivery as RECEIVED");
        }
        if (status == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("A new purchase order cannot start as CANCELLED");
        }

        return creators.stream()
                .filter(creator -> creator.supportedStatus() == status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No purchase-order creator is registered for " + status))
                .create(supplier, product, quantity, unitPrice, expectedDeliveryDate);
    }
}
