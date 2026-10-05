package com.greencart.service;

import com.greencart.designpattern.member2cart.strategy.DeliveryFeeContext;
import com.greencart.designpattern.member2cart.strategy.DeliveryFeeRules;
import com.greencart.entity.CartItem;
import com.greencart.entity.Product;
import com.greencart.entity.User;
import com.greencart.repository.CartItemRepository;
import com.greencart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** Shopping cart business logic - IT25103168 (Dunuwara H.G.M.K.C). */
@Service
@RequiredArgsConstructor
public class CartService {

    public static final BigDecimal FREE_DELIVERY_THRESHOLD = DeliveryFeeRules.FREE_DELIVERY_THRESHOLD;
    public static final BigDecimal STANDARD_DELIVERY_FEE = DeliveryFeeRules.STANDARD_DELIVERY_FEE;

    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;
    private final DeliveryFeeContext deliveryFeeContext;

    @Transactional
    public List<CartItem> items(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        List<CartItem> items = cartRepository.findByUser(user);
        List<CartItem> unavailable = items.stream()
                .filter(i -> i.getProduct() == null || i.getProduct().getDeletedAt() != null)
                .toList();
        if (!unavailable.isEmpty()) {
            cartRepository.deleteAll(unavailable);
            items = items.stream().filter(i -> !unavailable.contains(i)).toList();
        }
        return items;
    }

    public long count(User user) {
        return user == null ? 0L : items(user).size();
    }

    /** Total before product discounts are applied. */
    public BigDecimal originalTotal(User user) {
        if (user == null) {
            return BigDecimal.ZERO;
        }
        return items(user).stream()
                .map(CartItem::getOriginalSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Subtotal after product discounts are applied. */
    public BigDecimal total(User user) {
        if (user == null) {
            return BigDecimal.ZERO;
        }
        return items(user).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total amount saved through product discounts. */
    public BigDecimal discountSavings(User user) {
        return originalTotal(user).subtract(total(user));
    }

    public BigDecimal shippingFee(User user) {
        // Member 2 Strategy Pattern: the Context selects Free or Standard fee at runtime.
        return deliveryFeeContext.calculate(total(user));
    }

    public BigDecimal grandTotal(User user) {
        return total(user).add(shippingFee(user));
    }

    /**
     * Adds the requested quantity to the cart.
     *
     * Product-detail forms use setQuantity=true so choosing qty=3 stores exactly 3
     * even when that product was already in the cart. Quick "Add" buttons use
     * setQuantity=false and add one more item per click. The front-end blocks
     * accidental double-submit so a single click is processed once.
     *
     * @return the new quantity stored in the cart for this product.
     */
    @Transactional
    public int add(User user, Long productId, int qty, boolean setQuantity) {
        requireUser(user);
        if (qty < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product is not available"));
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new IllegalArgumentException("This product is out of stock");
        }

        CartItem item = cartRepository.findByUserAndProduct(user, product).orElse(null);
        int existingQty = item == null || item.getQuantity() == null ? 0 : item.getQuantity();
        int newQty = setQuantity ? qty : existingQty + qty;

        if (newQty > product.getStock()) {
            throw new IllegalArgumentException(
                    "Only " + product.getStock() + " item(s) are currently available. "
                            + "Your cart already has " + existingQty + ".");
        }

        if (item == null) {
            item = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(newQty)
                    .build();
        } else {
            item.setQuantity(newQty);
        }

        cartRepository.save(item);
        return newQty;
    }

    public CartItem update(User user, Long itemId, int qty) {
        CartItem item = ownedItem(user, itemId);
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (item.getProduct().getDeletedAt() != null) {
            throw new IllegalArgumentException("This product is no longer available");
        }
        if (item.getProduct().getStock() == null || qty > item.getProduct().getStock()) {
            throw new IllegalArgumentException(
                    "Only " + item.getProduct().getStock() + " item(s) are currently available");
        }
        item.setQuantity(qty);
        return cartRepository.save(item);
    }

    public void remove(User user, Long itemId) {
        cartRepository.delete(ownedItem(user, itemId));
    }

    @Transactional
    public void clear(User user) {
        if (user != null) {
            cartRepository.deleteByUser(user);
        }
    }

    private CartItem ownedItem(User user, Long itemId) {
        requireUser(user);
        CartItem item = cartRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        if (item.getUser() == null || !item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot modify another customer's cart");
        }
        return item;
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Please sign in before managing your cart");
        }
    }
}
