package org.os4all.modules.counselor.repository;

import org.os4all.modules.counselor.entity.CounselorConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CounselorConversationRepository extends JpaRepository<CounselorConversation, UUID> {
    Page<CounselorConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
    Optional<CounselorConversation> findByIdAndUserId(UUID id, UUID userId);
}
