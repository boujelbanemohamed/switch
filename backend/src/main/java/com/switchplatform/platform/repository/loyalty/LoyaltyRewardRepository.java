package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyReward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, UUID> {
    List<LoyaltyReward> findByProgramId(UUID programId);
    List<LoyaltyReward> findByProgramIdAndStatus(UUID programId, LoyaltyReward.RewardStatus status);
}
