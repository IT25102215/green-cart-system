package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 30)
    private String vehicleNumber;

    @Column(name = "availability_status", nullable = false, length = 30)
    @Builder.Default
    private String availabilityStatus = "AVAILABLE";

    /** Maximum number of simultaneous active deliveries allowed for this staff member. */
    @Column(name = "max_active_orders", nullable = false, columnDefinition = "INT DEFAULT 5")
    @Builder.Default
    private Integer maxActiveOrders = 5;

    @PrePersist
    @PreUpdate
    public void setDefaults() {
        if (maxActiveOrders == null || maxActiveOrders < 1) maxActiveOrders = 5;
        if (availabilityStatus == null || availabilityStatus.isBlank()) availabilityStatus = "AVAILABLE";
    }
}
