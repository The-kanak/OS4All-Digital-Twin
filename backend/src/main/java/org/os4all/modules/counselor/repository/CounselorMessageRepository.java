package org.os4all.modules.counselor.repository;

import org.os4all.modules.counselor.entity.CounselorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CounselorMessageRepository extends JpaRepository<CounselorMessage, UUID> {
    List<CounselorMessage> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);
}
