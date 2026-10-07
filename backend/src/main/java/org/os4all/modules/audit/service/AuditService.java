package org.os4all.modules.audit.service;

import org.os4all.modules.audit.entity.AuditLog;
import org.os4all.modules.audit.repository.AuditLogRepository;
import org.os4all.modules.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;
    private final boolean auditEnabled;

    public AuditService(AuditLogRepository auditLogRepository,
                        @Value("${app.audit.enabled:true}") boolean auditEnabled) {
        this.auditLogRepository = auditLogRepository;
        this.auditEnabled = auditEnabled;
    }

    @Transactional
    public void record(User user, String actor, String action, String resourceType, String resourceId, String ipAddress, String details) {
        if (!auditEnabled) {
            return;
        }

        try {
            String previousHash = auditLogRepository.findLatestLog()
                    .map(AuditLog::getIntegrityHash)
                    .orElse(GENESIS_HASH);

            Instant now = Instant.now();
            String hashInput = previousHash + "|" + actor + "|" + action + "|" + resourceType + "|" + resourceId + "|" + now.toEpochMilli();
            String integrityHash = computeSha256(hashInput);

            AuditLog auditLog = new AuditLog();
            auditLog.setUser(user);
            auditLog.setActor(actor);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setDetails(details);
            auditLog.setPreviousHash(previousHash);
            auditLog.setIntegrityHash(integrityHash);
            auditLog.setLoggedAt(now);

            auditLogRepository.save(auditLog);
            log.info("Audit logged: action={}, actor={}, resourceType={}, resourceId={}", action, actor, resourceType, resourceId);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
