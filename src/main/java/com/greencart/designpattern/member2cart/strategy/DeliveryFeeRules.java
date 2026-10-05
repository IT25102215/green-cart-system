package com.greencart.designpattern.member2cart.strategy;

import java.math.BigDecimal;

/** Shared values used by the cart delivery-fee Strategy implementations. */
public final class DeliveryFeeRules {
    public static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("5000.00");
    public static final BigDecimal STANDARD_DELIVERY_FEE = new BigDecimal("250.00");

    private DeliveryFeeRules() { }
}
