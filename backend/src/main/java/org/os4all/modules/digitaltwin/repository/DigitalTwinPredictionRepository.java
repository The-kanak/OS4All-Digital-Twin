package org.os4all.modules.digitaltwin.repository;

import org.os4all.modules.digitaltwin.entity.DigitalTwinPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigitalTwinPredictionRepository extends JpaRepository<DigitalTwinPrediction, UUID> {
    Optional<DigitalTwinPrediction> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    List<DigitalTwinPrediction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
