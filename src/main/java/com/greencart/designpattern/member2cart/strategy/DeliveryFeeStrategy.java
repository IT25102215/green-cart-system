package com.greencart.designpattern.member2cart.strategy;

import java.math.BigDecimal;

/** Strategy interface: interchangeable algorithms for calculating delivery fees. */
public interface DeliveryFeeStrategy {
    boolean supports(BigDecimal subtotal);
    BigDecimal calculate(BigDecimal subtotal);
    String getName();
}
