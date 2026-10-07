package org.os4all.modules.lab.repository;

import org.os4all.modules.lab.entity.LabResult;
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
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {

    @Query("SELECT r FROM LabResult r WHERE r.user.id = :userId " +
            "AND (:biomarker IS NULL OR LOWER(r.standardizedBiomarker) = LOWER(:biomarker) OR LOWER(r.biomarker) = LOWER(:biomarker)) " +
            "AND (:startDate IS NULL OR r.collectionDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.collectionDate <= :endDate) " +
            "ORDER BY r.collectionDate DESC")
    Page<LabResult> findFiltered(
            @Param("userId") UUID userId,
            @Param("biomarker") String biomarker,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    List<LabResult> findByLabReportId(UUID labReportId);

    List<LabResult> findByUserIdOrderByCollectionDateDesc(UUID userId);

    long countByUserId(UUID userId);
}
