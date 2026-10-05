package com.greencart.designpattern.member2cart.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Context class: delegates fee calculation to the selected Strategy at runtime. */
@Component
@RequiredArgsConstructor
public class DeliveryFeeContext {
    private final List<DeliveryFeeStrategy> strategies;

    public BigDecimal calculate(BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        return strategies.stream()
                .filter(strategy -> strategy.supports(safeSubtotal))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No delivery-fee strategy is available"))
                .calculate(safeSubtotal);
    }

    public String selectedStrategy(BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        return strategies.stream()
                .filter(strategy -> strategy.supports(safeSubtotal))
                .findFirst()
                .map(DeliveryFeeStrategy::getName)
                .orElse("NONE");
    }
}
