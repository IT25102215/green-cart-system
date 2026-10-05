package com.greencart.repository;

import com.greencart.entity.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {
    List<OrderAuditLog> findTop100ByOrderByCreatedAtDesc();
}
