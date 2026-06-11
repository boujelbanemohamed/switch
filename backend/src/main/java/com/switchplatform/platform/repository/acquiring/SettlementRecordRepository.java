package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID> {
    List<SettlementRecord> findByMerchantIdAndSettlementDateBetween(UUID merchantId, LocalDate from, LocalDate to);
    List<SettlementRecord> findByMerchantId(UUID merchantId);
    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
    List<SettlementRecord> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
