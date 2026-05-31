package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.MdrPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MdrPlanRepository extends JpaRepository<MdrPlan, UUID> {
    List<MdrPlan> findByMerchantId(UUID merchantId);
    Optional<MdrPlan> findByMerchantIdAndCardBrandAndCardType(UUID merchantId, String cardBrand, String cardType);
}
