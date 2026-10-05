package com.greencart.service;

import com.greencart.designpattern.member1product.observer.InventoryStockEvent;
import com.greencart.designpattern.member1product.observer.InventorySubject;
import com.greencart.designpattern.member2cart.strategy.DeliveryFeeContext;
import com.greencart.designpattern.member3order.observer.OrderStatusEvent;
import com.greencart.designpattern.member3order.observer.OrderStatusSubject;
import com.greencart.entity.*;
import com.greencart.repository.DeliveryRepository;
import com.greencart.repository.OrderRepository;
import com.greencart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final DeliveryRepository deliveryRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final InventorySubject inventorySubject;
    private final DeliveryFeeContext deliveryFeeContext;
    private final OrderStatusSubject orderStatusSubject;

    /**
     * Creates an order and reserves stock in one database transaction.
     * Products are locked with PESSIMISTIC_WRITE in deterministic id order so two
     * simultaneous checkouts cannot oversell the same stock.
     */
    @Transactional
    public Order createFromCart(User user, String address, String phone,
                                String paymentMethod, String deliveryNotes) {
        if (user == null) throw new IllegalArgumentException("Customer is required");

        List<CartItem> cartItems = cartService.items(user);
        if (cartItems.isEmpty()) throw new IllegalArgumentException("Cart is empty");

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        cartItems.stream()
                .map(ci -> ci.getProduct().getId())
                .distinct()
                .sorted()
                .forEach(id -> lockedProducts.put(id, productRepository.findForUpdateById(id)
                        .orElseThrow(() -> new IllegalArgumentException("A cart product is no longer available"))));

        for (CartItem cartItem : cartItems) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() < 1) {
                throw new IllegalArgumentException("Cart contains an invalid quantity");
            }
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (product.getDeletedAt() != null) {
                throw new IllegalArgumentException(product.getName() + " is no longer available");
            }
            int stock = product.getStock() == null ? 0 : product.getStock();
            if (cartItem.getQuantity() > stock) {
                throw new IllegalArgumentException(product.getName() + " has only " + stock + " item(s) left in stock");
            }
        }

        Order order = Order.builder()
                .user(user)
                .address(address)
                .phone(phone)
                .deliveryNotes(cleanOptional(deliveryNotes, 500))
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PENDING)
                .paymentStatus("COD".equalsIgnoreCase(paymentMethod) ? "PENDING" : "INITIATED")
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .originalPrice(product.getPrice())
                    .discountPercent(product.getDiscount() == null ? BigDecimal.ZERO : product.getDiscount())
                    .price(product.getFinalPrice())
                    .build();
            order.getItems().add(orderItem);
            subtotal = subtotal.add(orderItem.getLineSubtotal());
        }

        // Member 2 Strategy Pattern is reused by checkout so cart and order totals cannot diverge.
        BigDecimal deliveryFee = deliveryFeeContext.calculate(subtotal);
        order.setShippingFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));

        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            int oldStock = product.getStock() == null ? 0 : product.getStock();
            int newStock = oldStock - cartItem.getQuantity();
            if (newStock < 0) throw new IllegalStateException("Stock changed while checkout was processing");
            product.setStock(newStock);
            productRepository.save(product);
            inventorySubject.notifyObservers(new InventoryStockEvent(
                    product, oldStock, newStock, user.getEmail()
            ));
            auditService.logChange("INVENTORY", "ORDER_RESERVE", "Product", product.getId(),
                    String.valueOf(oldStock), String.valueOf(newStock),
                    "Order placement reduced stock for '" + product.getName() + "'",
                    user.getEmail());
        }

        Order savedOrder = orderRepository.save(order);
        publishStatus(savedOrder, null, user.getEmail(), "Order placed successfully");
        cartService.clear(user);
        return savedOrder;
    }

    public List<Order> mine(User user) { return orderRepository.findByUserOrderByCreatedAtDesc(user); }
    public List<Order> all() { return orderRepository.findAllByOrderByCreatedAtDesc(); }

    public Page<Order> searchAdmin(String q, OrderStatus status, LocalDate from, LocalDate to,
                                   int page, int size) {
        String clean = q == null || q.isBlank() ? null : q.trim();
        Long orderId = null;
        if (clean != null) {
            try {
                orderId = Long.parseLong(clean.startsWith("#") ? clean.substring(1) : clean);
            } catch (NumberFormatException ignored) {
                // Text search is still valid.
            }
        }
        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("To date cannot be before From date");
        }
        return orderRepository.searchAdmin(clean, orderId, status, fromDate, toDateExclusive,
                PageRequest.of(Math.max(page, 0), size, Sort.by("createdAt").descending()));
    }

    public Order get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public Order getForUser(Long id, User user) {
        Order order = get(id);
        if (user == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("This order does not belong to you");
        }
        return order;
    }

    public Order save(Order order) { return orderRepository.save(order); }

    @Transactional
    public Order approveByAdmin(Long orderId) {
        return approveByAdmin(orderId, "SYSTEM");
    }

    @Transactional
    public Order approveByAdmin(Long orderId, String actorEmail) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only a PENDING order can be approved");
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        publishStatus(saved, oldStatus, actorEmail, "Order approved and ready for delivery assignment");
        return saved;
    }

    @Transactional
    public Order cancelByAdmin(Long orderId, String reason, String actorEmail) {
        Order order = get(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("A delivered order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("An order already in delivery cannot be cancelled from Order Management");
        }
        String cleanReason = reason == null || reason.isBlank()
                ? "Order rejected/cancelled by administrator"
                : cleanRequired(reason, "Cancellation reason", 3, 500);
        return cancelInternal(order, cleanReason, actorEmail);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = get(orderId);
        OrderStatus current = order.getStatus();
        if (current == newStatus) return order;
        if (current == OrderStatus.CANCELLED) throw new IllegalArgumentException("A cancelled order cannot be reopened");
        if (current == OrderStatus.DELIVERED) throw new IllegalArgumentException("A delivered order cannot be changed");

        boolean allowed = switch (current) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalArgumentException("Invalid order status transition: " + current + " -> " + newStatus);
        if (newStatus == OrderStatus.CANCELLED) {
            return cancelInternal(order, "Order cancelled by administrator", "SYSTEM");
        }
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        publishStatus(saved, current, "SYSTEM", "Order status changed to " + newStatus);
        return saved;
    }

    @Transactional
    public Order returnToConfirmedForReassignment(Long orderId) {
        Order order = get(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("This order cannot be returned for reassignment");
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        publishStatus(saved, oldStatus, "SYSTEM", "Order returned for delivery reassignment");
        return saved;
    }

    @Transactional
    public Order cancelByCustomer(Long orderId, User user, String reason) {
        Order order = getForUser(orderId, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be cancelled by the customer");
        }
        return cancelInternal(order, cleanRequired(reason, "Cancellation reason", 3, 500), user.getEmail());
    }

    public ReorderResult reorderDeliveredOrder(Long orderId, User user) {
        Order order = getForUser(orderId, user);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Only DELIVERED orders can be reordered");
        }

        int addedUnits = 0;
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            String productName = item.getProduct() == null ? "Unavailable product" : item.getProduct().getName();
            if (item.getProduct() == null || item.getProduct().getId() == null) {
                skipped.add(productName);
                continue;
            }
            Optional<Product> activeProduct = productRepository.findByIdAndDeletedAtIsNull(item.getProduct().getId());
            if (activeProduct.isEmpty() || activeProduct.get().getStock() == null || activeProduct.get().getStock() <= 0) {
                skipped.add(productName);
                continue;
            }

            int requested = item.getQuantity() == null ? 1 : Math.max(item.getQuantity(), 1);
            int lineAdded = 0;
            for (int i = 0; i < requested; i++) {
                try {
                    cartService.add(user, activeProduct.get().getId(), 1, false);
                    addedUnits++;
                    lineAdded++;
                } catch (IllegalArgumentException ex) {
                    break;
                }
            }
            if (lineAdded < requested) {
                skipped.add(productName + " (only " + lineAdded + " of " + requested + " added)");
            }
        }
        return new ReorderResult(addedUnits, skipped);
    }

    @Transactional
    public Order updateDeliveryNotes(Long orderId, String notes) {
        Order order = get(orderId);
        order.setDeliveryNotes(cleanOptional(notes, 500));
        return orderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        Order order = get(id);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Only cancelled orders can be deleted");
        }
        orderRepository.delete(order);
    }

    public long count() { return orderRepository.count(); }
    public long countByStatus(OrderStatus status) { return orderRepository.countByStatus(status); }
    public BigDecimal totalIncome() { return orderRepository.totalIncome(); }
    public BigDecimal incomeSince(LocalDateTime since) { return orderRepository.incomeSince(since); }

    private Order cancelInternal(Order order, String reason, String actorEmail) {
        if (order.getStatus() == OrderStatus.CANCELLED) return order;
        OrderStatus oldStatus = order.getStatus();

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getId() != null && item.getQuantity() != null) {
                Product product = productRepository.findAnyForUpdateById(item.getProduct().getId())
                        .orElse(item.getProduct());
                int currentStock = product.getStock() == null ? 0 : product.getStock();
                int restoredStock = currentStock + item.getQuantity();
                product.setStock(restoredStock);
                productRepository.save(product);
                inventorySubject.notifyObservers(new InventoryStockEvent(
                        product, currentStock, restoredStock, actorEmail
                ));
                auditService.logChange("INVENTORY", "ORDER_STOCK_RESTORE", "Product", product.getId(),
                        String.valueOf(currentStock), String.valueOf(restoredStock),
                        "Order cancellation restored stock for '" + product.getName() + "'",
                        actorEmail == null || actorEmail.isBlank() ? "SYSTEM" : actorEmail);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) order.setPaymentStatus("CANCELLED");
        Order saved = orderRepository.save(order);
        publishStatus(saved, oldStatus, actorEmail, "Order cancelled. Reason: " + reason);

        deliveryRepository.findByOrder(order).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setProblem(reason);
            deliveryRepository.save(delivery);
        });
        return saved;
    }


    /** Member 3 Observer Pattern integration: one status change notifies many observers. */
    private void publishStatus(Order order, OrderStatus oldStatus, String actorEmail, String note) {
        if (order == null || order.getId() == null || order.getStatus() == null) return;
        User customer = order.getUser();
        orderStatusSubject.notifyObservers(new OrderStatusEvent(
                order.getId(),
                customer == null ? "Customer" : customer.getFullName(),
                customer == null ? null : customer.getEmail(),
                oldStatus,
                order.getStatus(),
                actorEmail,
                note
        ));
    }

    private String cleanRequired(String value, String label, int min, int max) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() < min) throw new IllegalArgumentException(label + " must be at least " + min + " characters");
        if (clean.length() > max) throw new IllegalArgumentException(label + " must not exceed " + max + " characters");
        return clean;
    }

    private String cleanOptional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException("Delivery notes must not exceed " + max + " characters");
        return clean;
    }

    public record ReorderResult(int addedUnits, List<String> skippedItems) {
        public boolean hasSkippedItems() { return skippedItems != null && !skippedItems.isEmpty(); }
    }
}
