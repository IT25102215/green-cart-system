package com.greencart.feature.cart;

import com.greencart.entity.CartItem;
import com.greencart.entity.User;
import com.greencart.service.CartService;
import com.greencart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * VIVA OWNER: IT25103168 - Dunuwara H.G.M.K.C.
 * MODULE: Shopping Cart Management
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public String view(Model model, Authentication authentication) {
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
        return "user/cart";
    }

    @PostMapping("/add/{productId}")
    public String add(@PathVariable Long productId,
                      @RequestParam(defaultValue = "1") int qty,
                      @RequestParam(defaultValue = "add") String mode,
                      Authentication authentication,
                      RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            boolean setQuantity = "set".equalsIgnoreCase(mode);
            int newQuantity = cartService.add(user, productId, qty, setQuantity);
            redirectAttributes.addFlashAttribute(
                    "success",
                    setQuantity
                            ? "Cart quantity updated to " + newQuantity + "."
                            : qty + " item(s) added to cart. Cart quantity is now " + newQuantity + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/shop/" + productId;
    }

    @PostMapping("/update/{itemId}")
    public String update(@PathVariable Long itemId,
                         @RequestParam int qty,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            cartService.update(user, itemId, qty);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/cart";
    }


    /**
     * AJAX quantity update used by the cart page. The quantity is persisted first,
     * then all cart totals are recalculated on the server and returned as JSON so
     * the page can update immediately without an extra "Update" button or reload.
     */
    @PostMapping("/update-ajax/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAjax(@PathVariable Long itemId,
                                                           @RequestParam int qty,
                                                           Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Please sign in before managing your cart"));
        }

        try {
            CartItem item = cartService.update(user, itemId, qty);
            BigDecimal originalTotal = cartService.originalTotal(user);
            BigDecimal subtotal = cartService.total(user);
            BigDecimal discountSavings = cartService.discountSavings(user);
            BigDecimal shippingFee = cartService.shippingFee(user);
            BigDecimal grandTotal = cartService.grandTotal(user);
            boolean freeDelivery = shippingFee.signum() == 0;
            BigDecimal remaining = CartService.FREE_DELIVERY_THRESHOLD
                    .subtract(subtotal)
                    .max(BigDecimal.ZERO);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("quantity", item.getQuantity());
            response.put("itemOriginalSubtotal", item.getOriginalSubtotal());
            response.put("itemSubtotal", item.getSubtotal());
            response.put("itemDiscountAmount", item.getDiscountAmount());
            response.put("originalTotal", originalTotal);
            response.put("discountSavings", discountSavings);
            response.put("subtotal", subtotal);
            response.put("shippingFee", shippingFee);
            response.put("grandTotal", grandTotal);
            response.put("freeDelivery", freeDelivery);
            response.put("freeDeliveryRemaining", remaining);
            response.put("cartCount", cartService.count(user));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", safeMessage(ex)));
        }
    }

    @PostMapping("/remove/{itemId}")
    public String remove(@PathVariable Long itemId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            cartService.remove(user, itemId);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/cart";
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
