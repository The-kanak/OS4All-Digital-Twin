package org.os4all.modules.digitaltwin.repository;

import org.os4all.modules.digitaltwin.entity.HistoricalMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricalMedicalRecordRepository extends JpaRepository<HistoricalMedicalRecord, UUID> {
    List<HistoricalMedicalRecord> findByUserIdOrderByDiagnosedDateDesc(UUID userId);
    List<HistoricalMedicalRecord> findByUserIdAndRecordTypeOrderByDiagnosedDateDesc(UUID userId, String recordType);
    void deleteByUserId(UUID userId);
    long countByUserId(UUID userId);
}
