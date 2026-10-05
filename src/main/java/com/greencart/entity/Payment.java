package com.greencart.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @OneToOne @JoinColumn(name="order_id") private Order order;
  private String method;
  private String status;
  private BigDecimal amount;
  private String transactionId;
  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
