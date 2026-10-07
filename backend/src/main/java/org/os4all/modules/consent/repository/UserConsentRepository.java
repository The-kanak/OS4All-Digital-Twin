package org.os4all.modules.consent.repository;

import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    List<UserConsent> findByUserId(UUID userId);
    Optional<UserConsent> findByUserIdAndConsentType(UUID userId, ConsentType consentType);
}
