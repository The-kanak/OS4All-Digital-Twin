package org.os4all.modules.consent.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.dto.ConsentResponse;
import org.os4all.modules.consent.entity.UserConsent;
import org.os4all.modules.consent.repository.UserConsentRepository;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ConsentService(UserConsentRepository consentRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getUserConsents(UUID userId) {
        return consentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsentResponse updateConsent(UUID userId, ConsentRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserConsent consent = consentRepository.findByUserIdAndConsentType(userId, request.getConsentType())
                .orElseGet(() -> {
                    UserConsent newConsent = new UserConsent();
                    newConsent.setUser(user);
                    newConsent.setConsentType(request.getConsentType());
                    return newConsent;
                });

        consent.setGranted(request.getGranted());
        consent.setIpAddress(ipAddress);
        UserConsent savedConsent = consentRepository.save(consent);

        auditService.record(
                user,
                user.getEmail(),
                "CONSENT_UPDATED",
                "CONSENT",
                request.getConsentType().name(),
                ipAddress,
                "Consent " + request.getConsentType() + " set to " + request.getGranted()
        );

        log.info("Consent {} updated to {} for user: {}", request.getConsentType(), request.getGranted(), userId);
        return mapToResponse(savedConsent);
    }

    private ConsentResponse mapToResponse(UserConsent consent) {
        return new ConsentResponse(
                consent.getId(),
                consent.getConsentType(),
                consent.isGranted(),
                consent.getGrantedAt(),
                consent.getRevokedAt()
        );
    }
}
