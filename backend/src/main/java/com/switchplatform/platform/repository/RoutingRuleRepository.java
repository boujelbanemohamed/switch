package com.switchplatform.platform.repository;

import com.switchplatform.platform.model.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    List<RoutingRule> findByStatusAndProtocolOrderByPriorityAsc(
            RoutingRule.RuleStatus status, RoutingRule.Protocol protocol);

    List<RoutingRule> findByStatusOrderByPriorityAsc(RoutingRule.RuleStatus status);
}
