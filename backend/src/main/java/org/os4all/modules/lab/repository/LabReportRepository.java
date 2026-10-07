package org.os4all.modules.lab.repository;

import org.os4all.modules.lab.entity.LabReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabReportRepository extends JpaRepository<LabReport, UUID> {

    @Query("SELECT r FROM LabReport r WHERE r.user.id = :userId " +
            "AND (:startDate IS NULL OR r.collectionDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.collectionDate <= :endDate) " +
            "ORDER BY r.collectionDate DESC")
    Page<LabReport> findFiltered(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("SELECT r FROM LabReport r WHERE r.user.id = :userId " +
            "AND (:startDate IS NULL OR r.collectionDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.collectionDate <= :endDate) " +
            "ORDER BY r.collectionDate DESC")
    List<LabReport> findTimelineReports(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    Optional<LabReport> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);

    long countByUserId(UUID userId);
}
