package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClearingRecordRepository extends JpaRepository<ClearingRecord, UUID> {
    List<ClearingRecord> findByClearingDate(LocalDate date);
    List<ClearingRecord> findByAcquiringParticipantIdOrIssuingParticipantId(UUID acquiringId, UUID issuingId);
    Optional<ClearingRecord> findByTransactionId(String transactionId);
    List<ClearingRecord> findByClearingDateAndStatus(LocalDate date, ClearingRecord.Status status);
    List<ClearingRecord> findByClearingDateAndAcquiringParticipantId(LocalDate date, UUID acquiringParticipantId);
    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
    List<ClearingRecord> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
