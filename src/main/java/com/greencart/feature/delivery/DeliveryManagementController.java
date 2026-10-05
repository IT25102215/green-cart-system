package com.greencart.feature.delivery;

import com.greencart.entity.*;
import com.greencart.repository.DeliveryStaffRepository;
import com.greencart.service.AuditService;
import com.greencart.service.DeliveryService;
import com.greencart.service.DeliveryStaffManagementService;
import com.greencart.service.OrderService;
import com.greencart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIVA OWNER: IT25102215 - Liyanage L.G.S.
 * MODULE: Delivery Management
 * DATABASE: delivery_staff + deliveries
 *
 * STRICT WORKFLOW:
 * PENDING order -> Order Admin approves -> CONFIRMED -> Delivery Manager assigns delivery staff ->
 * Delivery ASSIGNED -> staff progresses delivery -> Order SHIPPED -> Order DELIVERED.
 */
@Controller
@RequiredArgsConstructor
public class DeliveryManagementController {

    private final DeliveryService deliveryService;
    private final DeliveryStaffManagementService deliveryStaffManagementService;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping("/admin/deliveries")
    public String deliveries(@RequestParam(required = false) String q,
                             @RequestParam(required = false) DeliveryStatus status,
                             @RequestParam(required = false) Long staffId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        // Only currently active staff appear in the roster and assignment controls.
        List<DeliveryStaff> allStaff = deliveryStaffRepository.findByUser_EnabledTrueOrderByIdAsc();
        allStaff.forEach(deliveryService::refreshAvailability);

        Page<Delivery> deliveryPage = deliveryService.searchAdmin(q, status, staffId, page, 12);

        List<Order> approvedUnassignedOrders = orderService.all().stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                .filter(order -> deliveryService.byOrder(order)
                        .map(delivery -> delivery.getDeliveryStaff() == null
                                || delivery.getStatus() == DeliveryStatus.FAILED)
                        .orElse(true))
                .toList();

        Map<Long, Long> activeCounts = new HashMap<>();
        for (DeliveryStaff staff : allStaff) {
            activeCounts.put(staff.getId(), deliveryService.activeCount(staff));
        }

        model.addAttribute("deliveries", deliveryPage.getContent());
        model.addAttribute("deliveryPage", deliveryPage);
        model.addAttribute("page", deliveryPage.getNumber());
        model.addAttribute("totalPages", deliveryPage.getTotalPages());
        model.addAttribute("q", q);
        model.addAttribute("selectedDeliveryStatus", status);
        model.addAttribute("selectedStaffId", staffId);
        model.addAttribute("orders", approvedUnassignedOrders);
        model.addAttribute("deliveryPeople", allStaff);
        model.addAttribute("staffActiveCounts", activeCounts);
        return "admin/deliveries";
    }

    @PostMapping("/admin/deliveries/assign")
    public String assign(@RequestParam Long orderId,
                         @RequestParam Long deliveryStaffId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.get(orderId);
            DeliveryStaff staff = getValidStaff(deliveryStaffId);

            // Core rule: admin approval must happen BEFORE delivery assignment.
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalArgumentException(
                        "Only admin-approved (CONFIRMED) orders can be assigned to delivery staff");
            }

            Delivery saved = deliveryService.assign(order, staff);
            verifyAssignment(saved, staff);
            auditService.logChange("DELIVERY", "ASSIGN", "Delivery", saved.getId(),
                    "Unassigned", staff.getUser().getFullName(),
                    "Order #" + order.getId() + " assigned to delivery staff", authentication);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Order #" + order.getId() + " assigned to " + staff.getUser().getFullName()
                            + " (" + staff.getUser().getEmail() + "). "
                            + "Log in with that delivery staff account to see this order."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not assign delivery: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/assign-auto")
    public String autoAssign(@RequestParam Long orderId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.get(orderId);
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalArgumentException("Only admin-approved (CONFIRMED) orders can be assigned");
            }

            Delivery saved = deliveryService.autoAssign(order);
            DeliveryStaff selected = saved.getDeliveryStaff();
            if (selected == null || selected.getUser() == null) {
                throw new IllegalStateException("Automatic assignment did not select a delivery staff member");
            }
            auditService.logChange("DELIVERY", "AUTO_ASSIGN_STRATEGY", "Delivery", saved.getId(),
                    "Unassigned", selected.getUser().getFullName(),
                    "Least-loaded Strategy assigned order #" + order.getId(), authentication);
            redirectAttributes.addFlashAttribute("success",
                    "Strategy auto-assignment selected " + selected.getUser().getFullName()
                            + " for order #" + order.getId() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not auto-assign delivery: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    /**
     * Allows the admin to correct an assignment before delivery starts, or reassign
     * a failed delivery. This fixes the common demo case where the wrong staff member
     * was selected initially.
     */
    @PostMapping("/admin/deliveries/{id}/reassign")
    public String reassign(@PathVariable Long id,
                           @RequestParam Long deliveryStaffId,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            Delivery delivery = deliveryService.get(id);
            DeliveryStaff newStaff = getValidStaff(deliveryStaffId);

            if (delivery.getOrder() == null || delivery.getOrder().getStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalArgumentException("Only CONFIRMED orders can be assigned/reassigned");
            }

            if (delivery.getStatus() != DeliveryStatus.ASSIGNED
                    && delivery.getStatus() != DeliveryStatus.FAILED) {
                throw new IllegalArgumentException(
                        "Staff can only be changed before pickup starts, or after a FAILED delivery");
            }

            DeliveryStaff currentStaff = delivery.getDeliveryStaff();
            String oldStaffName = currentStaff == null || currentStaff.getUser() == null
                    ? "Unassigned" : currentStaff.getUser().getFullName();

            Delivery saved = deliveryService.assign(delivery.getOrder(), newStaff);
            verifyAssignment(saved, newStaff);
            auditService.logChange("DELIVERY", "REASSIGN", "Delivery", saved.getId(),
                    oldStaffName, newStaff.getUser().getFullName(),
                    "Order #" + delivery.getOrder().getId() + " reassigned", authentication);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Order #" + delivery.getOrder().getId() + " reassigned to "
                            + newStaff.getUser().getFullName() + " (" + newStaff.getUser().getEmail() + ")."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not reassign delivery: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }


    @PostMapping("/admin/deliveries/staff/create")
    public String createDeliveryStaff(@RequestParam String fullName,
                                      @RequestParam String email,
                                      @RequestParam String phone,
                                      @RequestParam String password,
                                      @RequestParam String vehicleNumber,
                                      @RequestParam(defaultValue = "5") int maxActiveOrders,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            DeliveryStaff staff = deliveryStaffManagementService.create(
                    fullName, email, phone, password, vehicleNumber, maxActiveOrders);
            auditService.logChange("DELIVERY", "CREATE_STAFF", "DeliveryStaff", staff.getId(),
                    null, staff.getUser().getEmail(),
                    "Created delivery staff " + staff.getUser().getFullName()
                            + " with vehicle " + staff.getVehicleNumber(), authentication);
            redirectAttributes.addFlashAttribute("success",
                    "Delivery staff account created for " + staff.getUser().getFullName() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not create delivery staff: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/staff/{id}/edit")
    public String editDeliveryStaff(@PathVariable Long id,
                                    @RequestParam String fullName,
                                    @RequestParam String email,
                                    @RequestParam String phone,
                                    @RequestParam String vehicleNumber,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            DeliveryStaff before = deliveryStaffManagementService.get(id);
            String oldValue = before.getUser().getFullName() + " | " + before.getUser().getEmail()
                    + " | " + before.getVehicleNumber();
            DeliveryStaff saved = deliveryStaffManagementService.update(id, fullName, email, phone, vehicleNumber);
            String newValue = saved.getUser().getFullName() + " | " + saved.getUser().getEmail()
                    + " | " + saved.getVehicleNumber();
            auditService.logChange("DELIVERY", "UPDATE_STAFF", "DeliveryStaff", id,
                    oldValue, newValue, "Updated delivery staff profile", authentication);
            redirectAttributes.addFlashAttribute("success",
                    "Delivery staff profile updated for " + saved.getUser().getFullName() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update delivery staff: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/staff/{id}/toggle")
    public String toggleDeliveryStaff(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            DeliveryStaff staff = deliveryStaffManagementService.get(id);
            boolean disabling = staff.getUser().isEnabled();
            if (disabling && deliveryService.activeCount(staff) > 0) {
                throw new IllegalArgumentException(
                        "Reassign or complete this staff member's active deliveries before disabling the account");
            }
            DeliveryStaff saved = deliveryStaffManagementService.setEnabled(id, !disabling);
            if (saved.getUser().isEnabled()) {
                deliveryService.refreshAvailability(saved);
            }
            auditService.logChange("DELIVERY", saved.getUser().isEnabled() ? "ENABLE_STAFF" : "DISABLE_STAFF",
                    "DeliveryStaff", id,
                    disabling ? "ENABLED" : "DISABLED",
                    saved.getUser().isEnabled() ? "ENABLED" : "DISABLED",
                    "Changed delivery staff account status for " + saved.getUser().getFullName(), authentication);
            redirectAttributes.addFlashAttribute("success",
                    saved.getUser().getFullName() + (saved.getUser().isEnabled() ? " enabled." : " disabled."));
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not change delivery staff status: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/staff/{id}/remove")
    public String removeDeliveryStaff(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            DeliveryStaff staff = deliveryStaffManagementService.get(id);
            if (deliveryService.activeCount(staff) > 0) {
                throw new IllegalArgumentException(
                        "Reassign or complete this staff member's active deliveries before removing the account");
            }
            String staffName = staff.getUser() == null ? "Delivery staff" : staff.getUser().getFullName();
            String staffEmail = staff.getUser() == null ? null : staff.getUser().getEmail();
            deliveryStaffManagementService.remove(id);
            auditService.logChange("DELIVERY", "REMOVE_STAFF", "DeliveryStaff", id,
                    staffEmail, null, "Removed delivery staff " + staffName, authentication);
            redirectAttributes.addFlashAttribute("success", staffName + " removed from delivery staff.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not remove delivery staff: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/staff/{id}/capacity")
    public String updateCapacity(@PathVariable Long id,
                                 @RequestParam int maxActiveOrders,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            DeliveryStaff staff = getValidStaff(id);
            int oldCapacity = staff.getMaxActiveOrders() == null ? 5 : staff.getMaxActiveOrders();
            DeliveryStaff saved = deliveryService.updateCapacity(staff, maxActiveOrders);
            auditService.logChange("DELIVERY", "CAPACITY_CHANGE", "DeliveryStaff", id,
                    String.valueOf(oldCapacity), String.valueOf(saved.getMaxActiveOrders()),
                    "Changed delivery capacity for " + saved.getUser().getFullName(), authentication);
            redirectAttributes.addFlashAttribute("success", "Delivery capacity updated for " + saved.getUser().getFullName() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update capacity: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/orders/{orderId}/note")
    public String updateDeliveryNote(@PathVariable Long orderId,
                                     @RequestParam(required = false) String deliveryNotes,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.get(orderId);
            String oldValue = order.getDeliveryNotes();
            Order saved = orderService.updateDeliveryNotes(orderId, deliveryNotes);
            auditService.logChange("DELIVERY", "NOTE_UPDATE", "Order", orderId,
                    oldValue, saved.getDeliveryNotes(), "Delivery note updated by management", authentication);
            redirectAttributes.addFlashAttribute("success", "Delivery note updated for order #" + orderId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update delivery note: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @PostMapping("/admin/deliveries/{id}/status")
    public String deliveryStatus(@PathVariable Long id,
                                 @RequestParam DeliveryStatus status,
                                 @RequestParam(required = false) String problem,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                 LocalDateTime rescheduledFor,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            updateDelivery(deliveryService.get(id), status, problem, rescheduledFor, authentication);
            redirectAttributes.addFlashAttribute("success", "Delivery status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update delivery: " + safeMessage(ex));
        }
        return "redirect:/admin/deliveries";
    }

    @GetMapping("/delivery/dashboard")
    public String deliveryDashboard(Model model, Authentication authentication) {
        User user = userService.getCurrent(authentication);
        if (user == null) {
            throw new IllegalStateException("Logged-in delivery user could not be resolved");
        }

        // Use USER ID as the source of truth for the logged-in staff member.
        DeliveryStaff staff = deliveryStaffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Delivery staff profile not found for account: " + user.getEmail()));

        deliveryService.refreshAvailability(staff);

        List<Delivery> myDeliveries = deliveryService.mineForUser(user);

        model.addAttribute("deliveryStaff", staff);
        model.addAttribute("deliveryPerson", user);
        model.addAttribute("deliveries", myDeliveries);
        model.addAttribute("assignedCount", myDeliveries.size());
        model.addAttribute("activeCount", deliveryService.activeCount(staff));
        model.addAttribute("completedCount", deliveryService.completedCount(staff));
        return "delivery/dashboard";
    }

    @PostMapping("/delivery/{id}/status")
    public String deliveryMine(@PathVariable Long id,
                               @RequestParam DeliveryStatus status,
                               @RequestParam(required = false) String problem,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                               LocalDateTime rescheduledFor,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User me = userService.getCurrent(authentication);
            if (me == null) {
                throw new IllegalArgumentException("Logged-in delivery user could not be resolved");
            }

            DeliveryStaff meStaff = deliveryStaffRepository.findByUserId(me.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Delivery staff profile not found"));

            Delivery delivery = deliveryService.get(id);
            if (delivery.getDeliveryStaff() == null
                    || !delivery.getDeliveryStaff().getId().equals(meStaff.getId())) {
                throw new IllegalArgumentException("You are not assigned to this delivery");
            }

            updateDelivery(delivery, status, problem, rescheduledFor, authentication);
            redirectAttributes.addFlashAttribute("success", "Delivery status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update delivery: " + safeMessage(ex));
        }
        return "redirect:/delivery/dashboard";
    }

    @GetMapping("/user/deliveries")
    public String customerDeliveries(Model model,
                                     Authentication authentication,
                                     @RequestParam(required = false) Long orderId) {
        User user = userService.getCurrent(authentication);
        List<Order> orders = orderService.mine(user);
        Map<Long, Delivery> deliveryMap = new HashMap<>();

        for (Order order : orders) {
            deliveryService.byOrder(order).ifPresent(delivery -> deliveryMap.put(order.getId(), delivery));
        }

        model.addAttribute("orders", orders);
        model.addAttribute("deliveryMap", deliveryMap);
        model.addAttribute("highlightOrderId", orderId);
        return "user/delivery-tracking";
    }

    private DeliveryStaff getValidStaff(Long deliveryStaffId) {
        DeliveryStaff staff = deliveryStaffRepository.findById(deliveryStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery staff not found"));

        if (staff.getUser() == null || staff.getUser().getRole() != Role.DELIVERY_PERSON) {
            throw new IllegalArgumentException("Selected staff profile is not linked to a delivery-person account");
        }
        if (!staff.getUser().isEnabled()) {
            throw new IllegalArgumentException("Selected delivery staff account is disabled");
        }
        return staff;
    }

    private void verifyAssignment(Delivery saved, DeliveryStaff expectedStaff) {
        if (saved.getDeliveryStaff() == null
                || !saved.getDeliveryStaff().getId().equals(expectedStaff.getId())) {
            throw new IllegalStateException("Delivery assignment was not saved correctly");
        }
    }

    private void updateDelivery(Delivery delivery,
                                DeliveryStatus status,
                                String problem,
                                LocalDateTime rescheduledFor,
                                Authentication authentication) {
        Order order = delivery.getOrder();
        DeliveryStatus oldStatus = delivery.getStatus();
        OrderStatus oldOrderStatus = order.getStatus();

        if (delivery.getDeliveryStaff() == null) {
            throw new IllegalArgumentException("This delivery has not been assigned to a staff member");
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new IllegalArgumentException("Admin must approve the order before delivery can start");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("A cancelled order cannot continue through delivery");
        }
        if (order.getStatus() == OrderStatus.DELIVERED && status != DeliveryStatus.DELIVERED) {
            throw new IllegalArgumentException("A delivered order cannot return to an active delivery status");
        }

        if (!isAllowedDeliveryTransition(delivery.getStatus(), status)) {
            throw new IllegalArgumentException(
                    "Invalid delivery status transition: " + delivery.getStatus() + " -> " + status);
        }

        String cleanProblem = problem == null || problem.isBlank() ? null : problem.trim();

        if ((status == DeliveryStatus.DELAYED || status == DeliveryStatus.FAILED)
                && cleanProblem == null) {
            throw new IllegalArgumentException("Please enter a reason for a delayed or failed delivery");
        }

        if (status == DeliveryStatus.RESCHEDULED && rescheduledFor == null) {
            throw new IllegalArgumentException("Please select the new delivery date and time");
        }

        delivery.setStatus(status);
        delivery.setProblem(cleanProblem);
        delivery.setRescheduledFor(status == DeliveryStatus.RESCHEDULED ? rescheduledFor : null);
        deliveryService.save(delivery);

        // Order state follows actual delivery progress, not assignment.
        if (status == DeliveryStatus.PICKED_UP
                || status == DeliveryStatus.OUT_FOR_DELIVERY
                || status == DeliveryStatus.DELAYED
                || status == DeliveryStatus.RESCHEDULED) {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
            }
        } else if (status == DeliveryStatus.DELIVERED) {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
            }
            if (orderService.get(order.getId()).getStatus() == OrderStatus.SHIPPED) {
                orderService.updateStatus(order.getId(), OrderStatus.DELIVERED);
            }
        } else if (status == DeliveryStatus.FAILED) {
            // Make the failed order available for admin reassignment.
            orderService.returnToConfirmedForReassignment(order.getId());
        }

        deliveryService.refreshAvailability(delivery.getDeliveryStaff());
        OrderStatus newOrderStatus = orderService.get(order.getId()).getStatus();
        if (oldOrderStatus != newOrderStatus) {
            auditService.logChange("ORDER", "DELIVERY_SYNC", "Order", order.getId(),
                    oldOrderStatus.name(), newOrderStatus.name(),
                    "Order status synchronized with delivery progress", authentication);
        }
        auditService.logChange("DELIVERY", "STATUS_CHANGE", "Delivery", delivery.getId(),
                oldStatus.name(), status.name(),
                "Delivery for order #" + order.getId()
                        + (cleanProblem == null ? "" : ". Reason: " + cleanProblem), authentication);
    }

    private boolean isAllowedDeliveryTransition(DeliveryStatus current, DeliveryStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case ASSIGNED -> next == DeliveryStatus.PICKED_UP
                    || next == DeliveryStatus.DELAYED
                    || next == DeliveryStatus.RESCHEDULED
                    || next == DeliveryStatus.FAILED;
            case PICKED_UP -> next == DeliveryStatus.OUT_FOR_DELIVERY
                    || next == DeliveryStatus.DELAYED
                    || next == DeliveryStatus.RESCHEDULED
                    || next == DeliveryStatus.FAILED;
            case OUT_FOR_DELIVERY -> next == DeliveryStatus.DELIVERED
                    || next == DeliveryStatus.DELAYED
                    || next == DeliveryStatus.RESCHEDULED
                    || next == DeliveryStatus.FAILED;
            case DELAYED -> next == DeliveryStatus.PICKED_UP
                    || next == DeliveryStatus.OUT_FOR_DELIVERY
                    || next == DeliveryStatus.RESCHEDULED
                    || next == DeliveryStatus.FAILED;
            case RESCHEDULED -> next == DeliveryStatus.ASSIGNED
                    || next == DeliveryStatus.PICKED_UP
                    || next == DeliveryStatus.OUT_FOR_DELIVERY
                    || next == DeliveryStatus.DELAYED
                    || next == DeliveryStatus.FAILED;
            case DELIVERED, FAILED -> false;
        };
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
