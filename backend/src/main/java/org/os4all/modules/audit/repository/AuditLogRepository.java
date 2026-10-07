package org.os4all.modules.audit.repository;

import org.os4all.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.loggedAt DESC LIMIT 1")
    Optional<AuditLog> findLatestLog();

    Page<AuditLog> findByUserIdOrderByLoggedAtDesc(UUID userId, Pageable pageable);
}
