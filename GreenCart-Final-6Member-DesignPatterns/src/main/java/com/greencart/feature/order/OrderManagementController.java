package com.greencart.feature.order;

import com.greencart.entity.*;
import com.greencart.repository.OrderAuditLogRepository;
import com.greencart.service.AuditService;
import com.greencart.service.DeliveryService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderManagementController {

    private final OrderService orderService;
    private final UserService userService;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final AuditService auditService;
    private final DeliveryService deliveryService;

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication authentication) {
        User user = userService.getCurrent(authentication);
        List<Order> orders = orderService.mine(user);
        Map<Long, Delivery> deliveryMap = new HashMap<>();
        orders.forEach(o -> deliveryService.byOrder(o).ifPresent(d -> deliveryMap.put(o.getId(), d)));
        model.addAttribute("orders", orders);
        model.addAttribute("deliveryMap", deliveryMap);
        return "user/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetails(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userService.getCurrent(authentication);
        Order order = orderService.getForUser(id, user);
        model.addAttribute("order", order);
        model.addAttribute("delivery", deliveryService.byOrder(order).orElse(null));
        return "user/order-details";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelMyOrder(@PathVariable Long id,
                                @RequestParam String cancellationReason,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrent(authentication);
            Order before = orderService.getForUser(id, user);
            OrderStatus oldStatus = before.getStatus();
            Order cancelled = orderService.cancelByCustomer(id, user, cancellationReason);
            auditService.logChange("ORDER", "CUSTOMER_CANCEL", "Order", id,
                    oldStatus.name(), cancelled.getStatus().name(),
                    "Customer cancelled order. Reason: " + cancelled.getCancellationReason(), authentication);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully. Reserved stock was returned.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/reorder")
    public String reorder(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrent(authentication);
            OrderService.ReorderResult result = orderService.reorderDeliveredOrder(id, user);
            if (result.addedUnits() == 0) {
                redirectAttributes.addFlashAttribute("error", "None of the products from this order are currently available.");
                return "redirect:/orders/" + id;
            }
            String message = result.addedUnits() + " item(s) added to your cart.";
            if (result.hasSkippedItems()) {
                message += " Some unavailable/limited-stock items were skipped: " + String.join(", ", result.skippedItems());
            }
            auditService.log("ORDER", "REORDER", "Order", id,
                    "Customer reordered " + result.addedUnits() + " item(s)", authentication);
            redirectAttributes.addFlashAttribute("success", message);
            return "redirect:/cart";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", safeMessage(ex));
            return "redirect:/orders/" + id;
        }
    }

    @GetMapping("/admin/orders")
    public String allOrders(@RequestParam(required = false) String q,
                            @RequestParam(required = false) OrderStatus status,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        if (from != null && to != null && to.isBefore(from)) {
            model.addAttribute("error", "To date cannot be before From date.");
            to = from;
        }
        Page<Order> orderPage = orderService.searchAdmin(q, status, from, to, page, 12);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("page", orderPage.getNumber());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("auditLogs", orderAuditLogRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/orders";
    }

    @PostMapping("/admin/orders/{id}/approve")
    public String approveOrder(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Order before = orderService.get(id);
            OrderStatus oldStatus = before.getStatus();
            Order saved = orderService.approveByAdmin(id, authentication == null ? "SYSTEM" : authentication.getName());
            auditService.logChange("ORDER", "APPROVE", "Order", id,
                    oldStatus.name(), saved.getStatus().name(),
                    "Order approved by management", authentication);
            redirectAttributes.addFlashAttribute("success", "Order #" + id + " approved and ready for delivery assignment.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not approve order: " + safeMessage(ex));
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String cancellationReason,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Order before = orderService.get(id);
            OrderStatus oldStatus = before.getStatus();
            Order saved = orderService.cancelByAdmin(id, cancellationReason, authentication == null ? "SYSTEM" : authentication.getName());
            deliveryService.byOrder(saved).ifPresent(d -> deliveryService.refreshAvailability(d.getDeliveryStaff()));
            auditService.logChange("ORDER", "CANCEL", "Order", id,
                    oldStatus.name(), saved.getStatus().name(),
                    "Order cancelled/rejected by management. Reason: " + saved.getCancellationReason(), authentication);
            redirectAttributes.addFlashAttribute("success", "Order #" + id + " cancelled/rejected and reserved stock restored.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not cancel order: " + safeMessage(ex));
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Order before = orderService.get(id);
            OrderStatus oldStatus = before.getStatus();
            Order saved;
            if (status == OrderStatus.CONFIRMED) {
                saved = orderService.approveByAdmin(id, authentication == null ? "SYSTEM" : authentication.getName());
            } else if (status == OrderStatus.CANCELLED) {
                saved = orderService.cancelByAdmin(id, "Order cancelled by administrator", authentication == null ? "SYSTEM" : authentication.getName());
            } else {
                throw new IllegalArgumentException("Use Delivery Management for SHIPPED/DELIVERED. Admin can only approve or cancel here.");
            }
            if (saved.getStatus() == OrderStatus.CANCELLED) {
                deliveryService.byOrder(saved).ifPresent(d -> deliveryService.refreshAvailability(d.getDeliveryStaff()));
            }
            auditService.logChange("ORDER", "STATUS_CHANGE", "Order", id,
                    oldStatus.name(), saved.getStatus().name(),
                    "Order status changed by management", authentication);
            redirectAttributes.addFlashAttribute("success", "Order status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update order: " + safeMessage(ex));
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/delivery-note")
    public String updateDeliveryNote(@PathVariable Long id,
                                     @RequestParam(required = false) String deliveryNotes,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Order before = orderService.get(id);
            String oldValue = before.getDeliveryNotes();
            Order saved = orderService.updateDeliveryNotes(id, deliveryNotes);
            auditService.logChange("DELIVERY", "NOTE_UPDATE", "Order", id,
                    oldValue, saved.getDeliveryNotes(), "Delivery note updated from Order Management", authentication);
            redirectAttributes.addFlashAttribute("success", "Delivery note updated for order #" + id + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update delivery note: " + safeMessage(ex));
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            orderService.delete(id);
            auditService.log("ORDER", "DELETE", "Order", id, "Cancelled order record deleted", authentication);
            redirectAttributes.addFlashAttribute("success", "Cancelled order deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete order: " + safeMessage(ex));
        }
        return "redirect:/admin/orders";
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
