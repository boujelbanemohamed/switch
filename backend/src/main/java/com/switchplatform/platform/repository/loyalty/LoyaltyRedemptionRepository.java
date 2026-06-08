package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoyaltyRedemptionRepository extends JpaRepository<LoyaltyRedemption, UUID> {
    List<LoyaltyRedemption> findByMembershipIdOrderByCreatedAtDesc(UUID membershipId);
    List<LoyaltyRedemption> findByStatus(LoyaltyRedemption.RedemptionStatus status);
}
