package com.greencart.repository;

import com.greencart.entity.Order;
import com.greencart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrder(Order order);
}
