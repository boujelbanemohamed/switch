package com.switchplatform.platform.repository.fraud;

import com.switchplatform.platform.model.fraud.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {
    List<FraudRule> findByStatusOrderByCreatedAtDesc(FraudRule.Status status);
    List<FraudRule> findByStatus(FraudRule.Status status);
}
