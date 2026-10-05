package com.greencart.designpattern.member4delivery.strategy;

import com.greencart.entity.DeliveryStaff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Context: chooses an assignment Strategy and delegates the selection algorithm. */
@Component
@RequiredArgsConstructor
public class DeliveryAssignmentContext {
    private final List<DeliveryAssignmentStrategy> strategies;

    public DeliveryStaff select(String strategyKey,
                                DeliveryStaff requestedStaff,
                                List<DeliveryStaff> candidates,
                                Map<Long, Long> activeCounts) {
        String key = strategyKey == null ? "MANUAL" : strategyKey.trim().toUpperCase(Locale.ROOT);
        return strategies.stream()
                .filter(strategy -> strategy.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown delivery assignment strategy: " + key))
                .selectStaff(requestedStaff, candidates, activeCounts);
    }
}
