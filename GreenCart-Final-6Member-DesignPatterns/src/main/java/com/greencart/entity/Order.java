package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at"),
        @Index(name = "idx_orders_status_created", columnList = "status,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  private User user;

  @OneToMany(
          mappedBy = "order",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.EAGER
  )
  @Builder.Default
  private List<OrderItem> items = new ArrayList<>();

  @Column(nullable = false)
  private BigDecimal total;

  @Column(nullable = false)
  @Builder.Default
  private BigDecimal shippingFee = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private OrderStatus status = OrderStatus.PENDING;

  @Column(length = 255)
  private String address;

  @Column(length = 10)
  private String phone;

  @Column(name = "delivery_notes", length = 500)
  private String deliveryNotes;

  @Column(name = "cancellation_reason", length = 500)
  private String cancellationReason;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  private String paymentMethod;

  private String paymentStatus;

  private String paymentId;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Transient
  public boolean isFreeShipping() {
    return shippingFee != null && shippingFee.signum() == 0;
  }

  @Transient
  public BigDecimal getSubtotal() {
    BigDecimal safeTotal = total == null ? BigDecimal.ZERO : total;
    BigDecimal safeShipping = shippingFee == null ? BigDecimal.ZERO : shippingFee;
    return safeTotal.subtract(safeShipping).max(BigDecimal.ZERO);
  }
}
