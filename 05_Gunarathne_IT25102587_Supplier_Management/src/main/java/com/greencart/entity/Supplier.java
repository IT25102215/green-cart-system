package com.greencart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_active", columnList = "active"),
        @Index(name = "idx_suppliers_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 254)
    private String email;

    @Column(length = 10)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 150)
    private String company;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
