package com.switchplatform.platform.repository.authorization;

import com.switchplatform.platform.model.authorization.CardLimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardLimitUsageRepository extends JpaRepository<CardLimitUsage, UUID> {
    List<CardLimitUsage> findByCardId(UUID cardId);
}
