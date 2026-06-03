package com.switchplatform.platform.repository.dispute;

import com.switchplatform.platform.model.dispute.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Dispute> findByStatusIn(List<Dispute.DisputeStatus> statuses);

    List<Dispute> findByTransactionId(String transactionId);

    List<Dispute> findByEvidenceDeadlineBefore(OffsetDateTime deadline);
}
