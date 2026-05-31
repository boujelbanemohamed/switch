package com.switchplatform.platform.repository.authorization;

import com.switchplatform.platform.model.authorization.HoldRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HoldRecordRepository extends JpaRepository<HoldRecord, UUID> {
    List<HoldRecord> findByCardIdAndStatus(String cardId, String status);
    List<HoldRecord> findByCardAccountIdAndStatus(String cardAccountId, String status);
    List<HoldRecord> findByStatusAndExpiresAtBefore(String status, Instant now);
}
