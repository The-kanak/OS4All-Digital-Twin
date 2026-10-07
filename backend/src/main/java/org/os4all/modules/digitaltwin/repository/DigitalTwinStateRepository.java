package org.os4all.modules.digitaltwin.repository;

import org.os4all.modules.digitaltwin.entity.DigitalTwinState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigitalTwinStateRepository extends JpaRepository<DigitalTwinState, UUID> {
    Optional<DigitalTwinState> findTopByUserIdOrderByLastUpdatedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
