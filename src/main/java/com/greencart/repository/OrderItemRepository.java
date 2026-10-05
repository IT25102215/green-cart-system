package com.greencart.repository;
import com.greencart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {}
