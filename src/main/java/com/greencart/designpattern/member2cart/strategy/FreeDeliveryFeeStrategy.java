package com.greencart.designpattern.member2cart.strategy;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Concrete Strategy used when the cart reaches the free-delivery threshold. */
@Component
@Order(1)
public class FreeDeliveryFeeStrategy implements DeliveryFeeStrategy {
    @Override
    public boolean supports(BigDecimal subtotal) {
        return subtotal != null && subtotal.compareTo(DeliveryFeeRules.FREE_DELIVERY_THRESHOLD) >= 0;
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public String getName() {
        return "FREE_DELIVERY";
    }
}
