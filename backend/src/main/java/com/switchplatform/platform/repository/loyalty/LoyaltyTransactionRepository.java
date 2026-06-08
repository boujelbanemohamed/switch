package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {
    List<LoyaltyTransaction> findByMembershipIdOrderByCreatedAtDesc(UUID membershipId);
    List<LoyaltyTransaction> findByMembershipIdAndTypeOrderByCreatedAtAsc(UUID membershipId, LoyaltyTransaction.TransactionType type);

    @Query("SELECT t FROM LoyaltyTransaction t WHERE t.membershipId = :membershipId AND t.type = 'EARN' AND t.remaining > 0 AND (t.expiresAt IS NULL OR t.expiresAt > :now) ORDER BY t.createdAt ASC")
    List<LoyaltyTransaction> findAvailableEarnBatches(@Param("membershipId") UUID membershipId, @Param("now") OffsetDateTime now);

    @Query("SELECT t FROM LoyaltyTransaction t WHERE t.type = 'EARN' AND t.remaining > 0 AND t.expiresAt IS NOT NULL AND t.expiresAt <= :now ORDER BY t.expiresAt ASC")
    List<LoyaltyTransaction> findExpiredEarnBatches(@Param("now") OffsetDateTime now);
}
