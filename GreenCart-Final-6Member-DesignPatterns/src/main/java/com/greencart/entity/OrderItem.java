package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name="order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name="order_id")
  private Order order;

  @ManyToOne(fetch=FetchType.EAGER)
  @JoinColumn(name="product_id")
  private Product product;

  private Integer quantity;

  /** Final discounted unit price captured at checkout. */
  @Column(precision = 12, scale = 2)
  private BigDecimal price;

  /** Original unit price captured at checkout for historical order details. */
  @Column(name = "original_price", precision = 12, scale = 2)
  private BigDecimal originalPrice;

  /** Discount percentage captured at checkout. */
  @Column(name = "discount_percent", precision = 5, scale = 2)
  private BigDecimal discountPercent;

  @Transient
  public BigDecimal getLineSubtotal() {
    if (price == null || quantity == null) return BigDecimal.ZERO;
    return price.multiply(BigDecimal.valueOf(quantity));
  }
}
