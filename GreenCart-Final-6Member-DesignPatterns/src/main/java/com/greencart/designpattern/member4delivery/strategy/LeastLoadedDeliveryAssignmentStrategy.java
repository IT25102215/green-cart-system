package com.greencart.designpattern.member4delivery.strategy;

import com.greencart.entity.DeliveryStaff;
import com.greencart.entity.Role;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Concrete Strategy: automatically select the enabled staff member with the lightest workload. */
@Component
public class LeastLoadedDeliveryAssignmentStrategy implements DeliveryAssignmentStrategy {
    @Override
    public String getKey() {
        return "LEAST_LOADED";
    }

    @Override
    public DeliveryStaff selectStaff(DeliveryStaff requestedStaff,
                                     List<DeliveryStaff> candidates,
                                     Map<Long, Long> activeCounts) {
        return candidates.stream()
                .filter(staff -> staff.getId() != null)
                .filter(staff -> staff.getUser() != null)
                .filter(staff -> staff.getUser().getRole() == Role.DELIVERY_PERSON)
                .filter(staff -> staff.getUser().isEnabled())
                .filter(staff -> activeCounts.getOrDefault(staff.getId(), 0L)
                        < (staff.getMaxActiveOrders() == null ? 5 : staff.getMaxActiveOrders()))
                .min(Comparator
                        .comparingLong((DeliveryStaff staff) -> activeCounts.getOrDefault(staff.getId(), 0L))
                        .thenComparing(DeliveryStaff::getId))
                .orElseThrow(() -> new IllegalArgumentException("No delivery staff member has free capacity"));
    }
}
