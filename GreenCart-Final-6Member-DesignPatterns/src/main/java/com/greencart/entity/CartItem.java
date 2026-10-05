package com.greencart.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="cart_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id") private User user;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="product_id") private Product product;
  @Column(nullable=false) private Integer quantity = 1;

  @Transient
  public java.math.BigDecimal getOriginalSubtotal() {
    if (product == null || product.getPrice() == null || quantity == null) {
      return java.math.BigDecimal.ZERO;
    }
    return product.getPrice().multiply(java.math.BigDecimal.valueOf(quantity));
  }

  @Transient
  public java.math.BigDecimal getSubtotal() {
    if (product == null || product.getFinalPrice() == null || quantity == null) {
      return java.math.BigDecimal.ZERO;
    }
    return product.getFinalPrice().multiply(java.math.BigDecimal.valueOf(quantity));
  }

  @Transient
  public java.math.BigDecimal getDiscountAmount() {
    return getOriginalSubtotal().subtract(getSubtotal());
  }
}
