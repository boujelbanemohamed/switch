package com.switchplatform.platform.repository.standin;

import com.switchplatform.platform.model.standin.StandInRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StandInRuleRepository extends JpaRepository<StandInRule, UUID> {
    List<StandInRule> findByIssuerParticipantId(UUID issuerParticipantId);
    List<StandInRule> findByEnabledTrue();
    Optional<StandInRule> findByIssuerParticipantIdAndCardBrand(UUID issuerParticipantId, String cardBrand);
    Optional<StandInRule> findByIssuerParticipantIdAndCardBrandAndEnabledTrue(UUID issuerParticipantId, String cardBrand);
    List<StandInRule> findByIssuerParticipantIdIsNullAndCardBrandAndEnabledTrue(String cardBrand);
}
