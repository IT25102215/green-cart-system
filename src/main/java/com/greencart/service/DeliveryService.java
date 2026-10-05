package com.greencart.service;

import com.greencart.designpattern.member4delivery.strategy.DeliveryAssignmentContext;
import com.greencart.entity.*;
import com.greencart.repository.DeliveryRepository;
import com.greencart.repository.DeliveryStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final EnumSet<DeliveryStatus> ACTIVE_STATUSES = EnumSet.of(
            DeliveryStatus.ASSIGNED,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.OUT_FOR_DELIVERY,
            DeliveryStatus.DELAYED,
            DeliveryStatus.RESCHEDULED
    );

    private final DeliveryRepository repo;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final DeliveryAssignmentContext deliveryAssignmentContext;

    public List<Delivery> all() { return repo.findAllByOrderByUpdatedAtDesc(); }

    public Page<Delivery> searchAdmin(String q, DeliveryStatus status, Long staffId, int page, int size) {
        String clean = q == null || q.isBlank() ? null : q.trim();
        return repo.searchAdmin(clean, status, staffId,
                PageRequest.of(Math.max(page, 0), size, Sort.by("updatedAt").descending()));
    }

    public List<Delivery> mine(DeliveryStaff staff) { return repo.findByDeliveryStaffOrderByUpdatedAtDesc(staff); }

    public List<Delivery> mineForUser(User user) {
        if (user == null || user.getId() == null) return List.of();
        return repo.findByDeliveryStaff_User_IdOrderByUpdatedAtDesc(user.getId());
    }

    public Delivery get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Delivery not found"));
    }

    @Transactional
    public Delivery save(Delivery delivery) {
        Delivery saved = repo.saveAndFlush(delivery);
        if (saved.getDeliveryStaff() != null) refreshAvailability(saved.getDeliveryStaff());
        return saved;
    }

    public Optional<Delivery> byOrder(Order order) { return repo.findByOrder(order); }

    /** A staff member may carry multiple active deliveries at the same time. */
    @Transactional
    public Delivery assign(Order order, DeliveryStaff staff) {
        return assignUsingStrategy(order, staff, "MANUAL");
    }

    /** Member 4 Strategy Pattern: automatically use the least-loaded eligible staff member. */
    @Transactional
    public Delivery autoAssign(Order order) {
        return assignUsingStrategy(order, null, "LEAST_LOADED");
    }

    private Delivery assignUsingStrategy(Order order, DeliveryStaff requestedStaff, String strategyKey) {
        if (order == null || order.getId() == null) throw new IllegalArgumentException("Order is required");

        Delivery delivery = byOrder(order).orElseGet(() -> Delivery.builder()
                .order(order)
                .status(DeliveryStatus.ASSIGNED)
                .build());

        DeliveryStaff oldStaff = delivery.getDeliveryStaff();
        List<DeliveryStaff> candidates = deliveryStaffRepository.findAllByOrderByIdAsc();
        Map<Long, Long> activeCounts = new HashMap<>();
        for (DeliveryStaff candidate : candidates) {
            activeCounts.put(candidate.getId(), activeCount(candidate));
        }

        // Re-selecting the same already-active assignment must not consume a second capacity slot.
        if (oldStaff != null && requestedStaff != null
                && oldStaff.getId().equals(requestedStaff.getId())
                && ACTIVE_STATUSES.contains(delivery.getStatus())) {
            activeCounts.computeIfPresent(oldStaff.getId(), (id, count) -> Math.max(0L, count - 1L));
        }

        DeliveryStaff staff = deliveryAssignmentContext.select(
                strategyKey, requestedStaff, candidates, activeCounts
        );
        if (staff.getUser() == null) throw new IllegalArgumentException("Selected delivery staff has no linked user account");

        delivery.setDeliveryStaff(staff);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setProblem(null);
        delivery.setRescheduledFor(null);
        delivery.setAssignedAt(LocalDateTime.now());

        Delivery saved = repo.saveAndFlush(delivery);
        if (oldStaff != null && !oldStaff.getId().equals(staff.getId())) refreshAvailability(oldStaff);
        refreshAvailability(staff);
        return saved;
    }

    public long activeCount(DeliveryStaff staff) {
        if (staff == null || staff.getId() == null) return 0;
        return repo.countByDeliveryStaffAndStatusIn(staff, ACTIVE_STATUSES);
    }

    public long completedCount(DeliveryStaff staff) {
        if (staff == null || staff.getId() == null) return 0;
        return repo.findByDeliveryStaffOrderByUpdatedAtDesc(staff).stream()
                .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                .count();
    }

    /**
     * Availability is informational only. ON_DUTY does not prevent new assignments.
     * This lets one staff member receive several deliveries for the same route/trip.
     */
    public void refreshAvailability(DeliveryStaff staff) {
        if (staff == null || staff.getId() == null) return;
        if (staff.getUser() != null && !staff.getUser().isEnabled()) {
            if (!"DISABLED".equalsIgnoreCase(staff.getAvailabilityStatus())) {
                staff.setAvailabilityStatus("DISABLED");
                deliveryStaffRepository.saveAndFlush(staff);
            }
            return;
        }
        long activeCount = activeCount(staff);
        String newStatus = activeCount > 0 ? "ON_DUTY" : "AVAILABLE";
        if (!newStatus.equalsIgnoreCase(staff.getAvailabilityStatus())) {
            staff.setAvailabilityStatus(newStatus);
            deliveryStaffRepository.saveAndFlush(staff);
        }
    }

    @Transactional
    public DeliveryStaff updateCapacity(DeliveryStaff staff, int capacity) {
        if (staff == null || staff.getId() == null) throw new IllegalArgumentException("Delivery staff is required");
        if (capacity < 1 || capacity > 20) throw new IllegalArgumentException("Capacity must be between 1 and 20");
        long active = activeCount(staff);
        if (capacity < active) {
            throw new IllegalArgumentException("Capacity cannot be lower than the current active delivery count (" + active + ")");
        }
        staff.setMaxActiveOrders(capacity);
        return deliveryStaffRepository.saveAndFlush(staff);
    }

    public long count() { return repo.count(); }
}
