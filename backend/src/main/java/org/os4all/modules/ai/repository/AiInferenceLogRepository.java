package org.os4all.modules.ai.repository;

import org.os4all.modules.ai.entity.AiInferenceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiInferenceLogRepository extends JpaRepository<AiInferenceLog, UUID> {
    Page<AiInferenceLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
