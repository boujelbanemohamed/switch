package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, UUID> {
    List<LoyaltyTier> findByProgramIdOrderByMinLifetimePointsAsc(UUID programId);
    List<LoyaltyTier> findByProgramIdAndStatus(UUID programId, LoyaltyTier.TierStatus status);
}
