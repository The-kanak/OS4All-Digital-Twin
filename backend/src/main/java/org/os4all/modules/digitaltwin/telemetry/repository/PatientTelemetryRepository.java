package org.os4all.modules.digitaltwin.telemetry.repository;

import org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientTelemetryRepository extends JpaRepository<PatientTelemetry, UUID> {

    Optional<PatientTelemetry> findTopByUserIdOrderByTimestampDesc(UUID userId);

    List<PatientTelemetry> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    @Query("SELECT t FROM PatientTelemetry t WHERE t.user.id = :userId AND t.timestamp >= :since ORDER BY t.timestamp ASC")
    List<PatientTelemetry> findRecentPoints(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT t FROM PatientTelemetry t WHERE t.user.id = :userId ORDER BY t.timestamp ASC")
    List<PatientTelemetry> findAllByUserIdAsc(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM PatientTelemetry t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);
}
