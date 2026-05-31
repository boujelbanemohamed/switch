package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, UUID> {
    Optional<ReconciliationRecord> findByReconciliationDateAndParticipantIdAndSource(LocalDate date, UUID participantId, ReconciliationRecord.Source source);
    List<ReconciliationRecord> findByReconciliationDate(LocalDate date);
}
