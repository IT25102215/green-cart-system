package com.greencart.controller;

import com.greencart.entity.OrderStatus;
import com.greencart.service.AuditService;
import com.greencart.service.OrderService;
import com.greencart.service.ProductService;
import com.greencart.service.ReviewService;
import com.greencart.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final AuditService auditService;

    @Value("${app.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalProducts", productService.count());
        model.addAttribute("totalOrders", orderService.count());
        model.addAttribute("totalReviews", reviewService.count());
        model.addAttribute("pendingOrders", orderService.countByStatus(OrderStatus.PENDING));
        model.addAttribute("confirmedOrders", orderService.countByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("shippedOrders", orderService.countByStatus(OrderStatus.SHIPPED));
        model.addAttribute("deliveredOrders", orderService.countByStatus(OrderStatus.DELIVERED));
        model.addAttribute("cancelledOrders", orderService.countByStatus(OrderStatus.CANCELLED));

        var lowStock = productService.lowStock(lowStockThreshold);
        var outOfStock = productService.outOfStock();
        model.addAttribute("lowStockThreshold", lowStockThreshold);
        model.addAttribute("lowStockCount", lowStock.size());
        model.addAttribute("outOfStockCount", outOfStock.size());
        model.addAttribute("lowStockPreview", lowStock.stream().limit(5).toList());

        BigDecimal totalIncome = Optional.ofNullable(orderService.totalIncome()).orElse(BigDecimal.ZERO);
        BigDecimal daily = Optional.ofNullable(orderService.incomeSince(LocalDateTime.now().minusDays(1)))
                .orElse(BigDecimal.ZERO);
        BigDecimal monthly = Optional.ofNullable(orderService.incomeSince(LocalDateTime.now().minusDays(30)))
                .orElse(BigDecimal.ZERO);

        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("dailyIncome", daily);
        model.addAttribute("monthlyIncome", monthly);

        // Dashboard analytics: last 6 months, status distribution and top delivered products.
        var allOrders = orderService.all();
        List<String> monthlyLabels = new ArrayList<>();
        List<Long> monthlyOrderCounts = new ArrayList<>();
        List<BigDecimal> monthlyRevenueSeries = new ArrayList<>();
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth currentMonth = YearMonth.now();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            monthlyLabels.add(month.format(monthFormat));
            long count = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(month))
                    .count();
            BigDecimal revenue = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(month))
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(o -> o.getTotal() == null ? BigDecimal.ZERO : o.getTotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyOrderCounts.add(count);
            monthlyRevenueSeries.add(revenue);
        }

        List<String> orderStatusLabels = new ArrayList<>();
        List<Long> orderStatusCounts = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            orderStatusLabels.add(status.name());
            orderStatusCounts.add(orderService.countByStatus(status));
        }

        Map<String, Integer> productTotals = new LinkedHashMap<>();
        allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getProduct() != null && i.getProduct().getName() != null)
                .forEach(i -> productTotals.merge(i.getProduct().getName(), i.getQuantity() == null ? 0 : i.getQuantity(), Integer::sum));
        var topProducts = productTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .toList();

        model.addAttribute("monthlyLabels", monthlyLabels);
        model.addAttribute("monthlyOrderCounts", monthlyOrderCounts);
        model.addAttribute("monthlyRevenueSeries", monthlyRevenueSeries);
        model.addAttribute("orderStatusLabels", orderStatusLabels);
        model.addAttribute("orderStatusCounts", orderStatusCounts);
        model.addAttribute("topProductLabels", topProducts.stream().map(Map.Entry::getKey).toList());
        model.addAttribute("topProductQuantities", topProducts.stream().map(Map.Entry::getValue).toList());
        return "admin/dashboard";
    }

    @GetMapping("/visit-site")
    public String visitCustomerSite(HttpSession session) {
        session.setAttribute("customerSitePreview", true);
        return "redirect:/";
    }

    @GetMapping("/exit-site-preview")
    public String exitCustomerSitePreview(HttpSession session) {
        session.removeAttribute("customerSitePreview");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users")
    public String usersPage(Model model) {
        model.addAttribute("users", userService.all());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, Authentication authentication) {
        var user = userService.get(id);
        boolean wasEnabled = user.isEnabled();
        userService.toggleEnabled(id);
        auditService.log("SECURITY", wasEnabled ? "BLOCK_USER" : "UNBLOCK_USER", "User", id,
                (wasEnabled ? "Blocked" : "Unblocked") + " user " + user.getEmail(), authentication);
        return "redirect:/admin/users";
    }

    @GetMapping("/reviews")
    public String reviewsPage(Model model) {
        model.addAttribute("reviews", reviewService.all());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id) {
        reviewService.delete(id);
        return "redirect:/admin/reviews";
    }

    @GetMapping("/income")
    public String income(Model model) {
        BigDecimal daily = Optional.ofNullable(orderService.incomeSince(LocalDateTime.now().minusDays(1)))
                .orElse(BigDecimal.ZERO);
        BigDecimal weekly = Optional.ofNullable(orderService.incomeSince(LocalDateTime.now().minusDays(7)))
                .orElse(BigDecimal.ZERO);
        BigDecimal monthly = Optional.ofNullable(orderService.incomeSince(LocalDateTime.now().minusDays(30)))
                .orElse(BigDecimal.ZERO);
        BigDecimal total = Optional.ofNullable(orderService.totalIncome()).orElse(BigDecimal.ZERO);

        model.addAttribute("daily", daily);
        model.addAttribute("weekly", weekly);
        model.addAttribute("monthly", monthly);
        model.addAttribute("total", total);
        return "admin/income";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", auditService.recent());
        return "admin/audit-logs";
    }
}
