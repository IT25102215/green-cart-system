package com.greencart.designpattern.member2cart.strategy;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Concrete Strategy used for normal carts below the free-delivery threshold. */
@Component
@Order(2)
public class StandardDeliveryFeeStrategy implements DeliveryFeeStrategy {
    @Override
    public boolean supports(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return DeliveryFeeRules.STANDARD_DELIVERY_FEE;
    }

    @Override
    public String getName() {
        return "STANDARD_DELIVERY";
    }
}
