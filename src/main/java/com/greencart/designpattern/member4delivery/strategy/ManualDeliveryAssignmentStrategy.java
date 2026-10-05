package com.greencart.designpattern.member4delivery.strategy;

import com.greencart.entity.DeliveryStaff;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Concrete Strategy: use the staff member selected by the administrator. */
@Component
public class ManualDeliveryAssignmentStrategy implements DeliveryAssignmentStrategy {
    @Override
    public String getKey() {
        return "MANUAL";
    }

    @Override
    public DeliveryStaff selectStaff(DeliveryStaff requestedStaff,
                                     List<DeliveryStaff> candidates,
                                     Map<Long, Long> activeCounts) {
        if (requestedStaff == null || requestedStaff.getId() == null) {
            throw new IllegalArgumentException("Delivery staff is required for manual assignment");
        }
        long active = activeCounts.getOrDefault(requestedStaff.getId(), 0L);
        int capacity = requestedStaff.getMaxActiveOrders() == null ? 5 : requestedStaff.getMaxActiveOrders();
        if (active >= capacity) {
            throw new IllegalArgumentException(requestedStaff.getUser().getFullName()
                    + " already has " + active + " active deliveries (capacity: " + capacity + ")");
        }
        return requestedStaff;
    }
}
