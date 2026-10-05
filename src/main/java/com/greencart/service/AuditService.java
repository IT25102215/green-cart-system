package com.greencart.service;

import com.greencart.entity.SystemAuditLog;
import com.greencart.repository.SystemAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final SystemAuditLogRepository repo;

    public void log(String module, String action, String entityType, Long entityId,
                    String description, Authentication authentication) {
        String actor = authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();
        log(module, action, entityType, entityId, description, actor);
    }

    public void log(String module, String action, String entityType, Long entityId,
                    String description, String actorEmail) {
        save(module, action, entityType, entityId, null, null, description, actorEmail);
    }

    public void logChange(String module, String action, String entityType, Long entityId,
                          String oldValue, String newValue, String description,
                          Authentication authentication) {
        String actor = authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();
        logChange(module, action, entityType, entityId, oldValue, newValue, description, actor);
    }

    public void logChange(String module, String action, String entityType, Long entityId,
                          String oldValue, String newValue, String description,
                          String actorEmail) {
        save(module, action, entityType, entityId, oldValue, newValue, description, actorEmail);
    }

    private void save(String module, String action, String entityType, Long entityId,
                      String oldValue, String newValue, String description, String actorEmail) {
        repo.save(SystemAuditLog.builder()
                .module(module)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description == null ? "" : description)
                .actorEmail(actorEmail == null || actorEmail.isBlank() ? "SYSTEM" : actorEmail)
                .build());
    }

    public List<SystemAuditLog> recent() {
        return repo.findTop200ByOrderByCreatedAtDesc();
    }

    public List<SystemAuditLog> recentByModule(String module) {
        return repo.findTop50ByModuleOrderByCreatedAtDesc(module);
    }
}
