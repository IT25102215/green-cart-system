package com.greencart.feature.order;

import com.greencart.entity.Order;
import com.greencart.entity.OrderStatus;
import com.greencart.entity.Payment;
import com.greencart.entity.Role;
import com.greencart.entity.User;
import com.greencart.repository.PaymentRepository;
import com.greencart.service.CartService;
import com.greencart.service.OrderService;
import com.greencart.util.PhoneValidation;
import com.greencart.util.InputValidation;
import com.greencart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Checkout is part of Order Management.
 * VIVA OWNER: IT25100048 - Pawantha L.H.K.
 */
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final UserService userService;
    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;

    @Value("${app.stripe.publishable-key}")
    private String stripeKey;

    @GetMapping("/checkout")
    public String checkout(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("items", cartService.items(user));
        model.addAttribute("originalTotal", cartService.originalTotal(user));
        model.addAttribute("discountSavings", cartService.discountSavings(user));
        model.addAttribute("total", cartService.total(user));
        var shippingFee = cartService.shippingFee(user);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", cartService.grandTotal(user));
        model.addAttribute("freeDeliveryThreshold", CartService.FREE_DELIVERY_THRESHOLD);
        model.addAttribute("freeDelivery", shippingFee.signum() == 0);
        model.addAttribute("user", user);
        model.addAttribute("stripeKey", stripeKey);
        return "user/checkout";
    }

    @PostMapping("/checkout")
    public String place(@RequestParam String address,
                        @RequestParam String phone,
                        @RequestParam(required = false) String deliveryNotes,
                        @RequestParam(defaultValue = "COD") String paymentMethod,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() != Role.USER) {
            return "redirect:/dashboard";
        }
        final String validatedAddress;
        final String validatedPhone;
        try {
            validatedAddress = InputValidation.requireText(address, "Delivery address", 5, 255);
            validatedPhone = PhoneValidation.requireTenDigits(phone);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/checkout";
        }

        try {
            // Checkout starts with the customer profile details. If the customer edits
            // the delivery address or phone here, keep the latest values as the new
            // defaults so the next order does not require retyping them.
            boolean profileChanged = !validatedAddress.equals(user.getAddress())
                    || !validatedPhone.equals(user.getPhone());
            if (profileChanged) {
                user.setAddress(validatedAddress);
                user.setPhone(validatedPhone);
                userService.save(user);
            }

            String normalizedPaymentMethod = "ONLINE".equalsIgnoreCase(paymentMethod) ? "ONLINE" : "COD";
            Order order = orderService.createFromCart(
                    user,
                    validatedAddress,
                    validatedPhone,
                    normalizedPaymentMethod,
                    deliveryNotes);

            if ("COD".equals(normalizedPaymentMethod)) {
                return "redirect:/payment/success?orderId=" + order.getId();
            }
            return "redirect:/payment/online?orderId=" + order.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not place order. " + safeMessage(ex));
            return "redirect:/checkout";
        }
    }

    @GetMapping("/payment/online")
    public String online(@RequestParam Long orderId,
                         Model model,
                         Authentication authentication) {
        User user = currentUser(authentication);
        Order order = orderService.getForUser(orderId, user);
        model.addAttribute("order", order);
        model.addAttribute("stripeKey", stripeKey);
        return "user/payment-online";
    }

    @PostMapping("/payment/process")
    public String process(@RequestParam Long orderId,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(authentication);
            Order order = orderService.getForUser(orderId, user);
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("A cancelled order cannot be paid");
            }
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                return "redirect:/payment/success?orderId=" + order.getId();
            }

            // Demo online payment: simulated successful transaction.
            // For production, replace this block with Stripe PaymentIntent confirmation.
            String transactionId = "TXN-" + UUID.randomUUID();
            order.setPaymentStatus("PAID");
            order.setPaymentId(transactionId);
            // Payment success does NOT approve the order.
            // The order remains PENDING until an administrator explicitly approves it.
            order.setStatus(OrderStatus.PENDING);
            orderService.save(order);

            if (!paymentRepository.existsByOrder(order)) {
                paymentRepository.save(Payment.builder()
                        .order(order)
                        .method("ONLINE")
                        .status("PAID")
                        .amount(order.getTotal())
                        .transactionId(transactionId)
                        .build());
            }
            return "redirect:/payment/success?orderId=" + order.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Payment could not be completed: " + safeMessage(ex));
            return "redirect:/payment/failed";
        }
    }

    @GetMapping("/payment/success")
    public String success(@RequestParam Long orderId,
                          Model model,
                          Authentication authentication) {
        User user = currentUser(authentication);
        model.addAttribute("order", orderService.getForUser(orderId, user));
        return "user/payment-success";
    }

    @GetMapping("/payment/cancel")
    public String cancel() {
        return "user/payment-cancel";
    }

    @GetMapping("/payment/failed")
    public String failed() {
        return "user/payment-failed";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getCurrent(authentication);
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
