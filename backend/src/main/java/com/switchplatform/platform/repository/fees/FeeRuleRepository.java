package com.switchplatform.platform.repository.fees;

import com.switchplatform.platform.model.fees.FeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRule, UUID> {

    List<FeeRule> findByScheduleIdOrderByRuleOrderAsc(UUID scheduleId);

    List<FeeRule> findByBrandFilterAndCardTypeFilter(String brand, String cardType);

    void deleteByScheduleId(UUID scheduleId);
}
