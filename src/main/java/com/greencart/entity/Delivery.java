package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries", indexes = {
        @Index(name = "idx_deliveries_staff", columnList = "delivery_staff_id"),
        @Index(name = "idx_deliveries_status", columnList = "status"),
        @Index(name = "idx_deliveries_staff_status", columnList = "delivery_staff_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_staff_id")
    private DeliveryStaff deliveryStaff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;

    @Column(length = 500)
    private String problem;

    private LocalDateTime rescheduledFor;

    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
