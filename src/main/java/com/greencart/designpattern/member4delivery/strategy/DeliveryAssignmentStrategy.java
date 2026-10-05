package com.greencart.designpattern.member4delivery.strategy;

import com.greencart.entity.DeliveryStaff;

import java.util.List;
import java.util.Map;

/** Strategy interface for selecting delivery staff. */
public interface DeliveryAssignmentStrategy {
    String getKey();
    DeliveryStaff selectStaff(DeliveryStaff requestedStaff,
                              List<DeliveryStaff> candidates,
                              Map<Long, Long> activeCounts);
}
