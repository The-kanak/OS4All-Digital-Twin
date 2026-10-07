package org.os4all.modules.ingestion.repository;

import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthObservationRepository extends JpaRepository<HealthObservation, UUID> {

    @Query("SELECT o FROM HealthObservation o WHERE o.user.id = :userId " +
            "AND (:obsType IS NULL OR o.observationType = :obsType) " +
            "AND (:startDate IS NULL OR o.timestamp >= :startDate) " +
            "AND (:endDate IS NULL OR o.timestamp <= :endDate) " +
            "ORDER BY o.timestamp DESC")
    Page<HealthObservation> findFiltered(
            @Param("userId") UUID userId,
            @Param("obsType") ObservationType obsType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("SELECT o FROM HealthObservation o WHERE o.user.id = :userId " +
            "AND (:startDate IS NULL OR o.timestamp >= :startDate) " +
            "AND (:endDate IS NULL OR o.timestamp <= :endDate) " +
            "ORDER BY o.timestamp DESC")
    List<HealthObservation> findTimelineObservations(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT o FROM HealthObservation o WHERE o.user.id = :userId " +
            "AND (:startDate IS NULL OR o.timestamp >= :startDate) " +
            "ORDER BY o.timestamp ASC")
    List<HealthObservation> findObservationsForBaseline(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate
    );

    void deleteByUserId(UUID userId);

    long countByUserId(UUID userId);
}
