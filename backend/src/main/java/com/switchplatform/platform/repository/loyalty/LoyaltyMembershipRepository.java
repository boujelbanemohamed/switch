package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyMembershipRepository extends JpaRepository<LoyaltyMembership, UUID> {
    Optional<LoyaltyMembership> findByCardholderIdAndProgramId(UUID cardholderId, UUID programId);
    List<LoyaltyMembership> findByCardholderId(UUID cardholderId);
    List<LoyaltyMembership> findByProgramId(UUID programId);
    List<LoyaltyMembership> findByTierId(UUID tierId);
}
