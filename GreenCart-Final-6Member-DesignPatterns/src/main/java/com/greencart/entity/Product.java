package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "category")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 2000)
  private String description;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(precision = 5, scale = 2)
  private BigDecimal discount;

  @Column(nullable = false)
  private Integer stock = 0;

  @Column(length = 255)
  private String image;

  /** Soft delete marker. Null means the product is active. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  @PreUpdate
  public void setDefaults() {
    if (discount == null) discount = BigDecimal.ZERO;
    if (stock == null) stock = 0;
    if (price == null) price = BigDecimal.ZERO;
  }

  @Transient
  public boolean isInStock() {
    return deletedAt == null && stock != null && stock > 0;
  }

  @Transient
  public boolean isDeleted() {
    return deletedAt != null;
  }

  @Transient
  public BigDecimal getFinalPrice() {
    if (price == null) return BigDecimal.ZERO;
    if (discount == null || discount.signum() <= 0) return price;

    BigDecimal off = price
            .multiply(discount)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

    return price.subtract(off).setScale(2, RoundingMode.HALF_UP);
  }

  @Transient
  public boolean isOnSale() {
    return discount != null && discount.signum() > 0;
  }
}
