package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID> {
    List<SettlementRecord> findByMerchantIdAndSettlementDateBetween(String merchantId, LocalDate from, LocalDate to);
    List<SettlementRecord> findByMerchantId(String merchantId);
}
