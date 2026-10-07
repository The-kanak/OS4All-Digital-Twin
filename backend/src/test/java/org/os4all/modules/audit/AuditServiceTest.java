package org.os4all.modules.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.os4all.modules.audit.entity.AuditLog;
import org.os4all.modules.audit.repository.AuditLogRepository;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.user.entity.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @org.junit.jupiter.api.BeforeEach
    void initService() {
        auditService = new AuditService(auditLogRepository, true);
    }

    @Test
    @DisplayName("Should save audit log with correct integrity hash chain")
    void shouldSaveAuditLogWithHashChain() {
        when(auditLogRepository.findLatestLog()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.record(null, "test@os4all.com", "USER_REGISTERED", "USER", "some-id", "127.0.0.1", "test details");

        verify(auditLogRepository).save(argThat(log -> {
            assertNotNull(log.getIntegrityHash());
            assertFalse(log.getIntegrityHash().isEmpty());
            assertEquals(64, log.getIntegrityHash().length()); // SHA-256 hex is 64 chars
            assertEquals("USER_REGISTERED", log.getAction());
            assertEquals("USER", log.getResourceType());
            return true;
        }));
    }

    @Test
    @DisplayName("Should chain hash from previous audit log")
    void shouldChainHashFromPreviousLog() {
        AuditLog previousLog = new AuditLog();
        previousLog.setIntegrityHash("abc123previoushash000000000000000000000000000000000000000000000000");

        when(auditLogRepository.findLatestLog()).thenReturn(Optional.of(previousLog));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.record(null, "actor", "ACTION", "RESOURCE", "id", "ip", "details");

        verify(auditLogRepository).save(argThat(log -> {
            assertEquals("abc123previoushash000000000000000000000000000000000000000000000000", log.getPreviousHash());
            assertNotEquals(log.getPreviousHash(), log.getIntegrityHash());
            return true;
        }));
    }
}
