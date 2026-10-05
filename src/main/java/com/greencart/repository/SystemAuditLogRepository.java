package com.greencart.repository;

import com.greencart.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {
    List<SystemAuditLog> findTop200ByOrderByCreatedAtDesc();
    List<SystemAuditLog> findTop50ByModuleOrderByCreatedAtDesc(String module);
}
