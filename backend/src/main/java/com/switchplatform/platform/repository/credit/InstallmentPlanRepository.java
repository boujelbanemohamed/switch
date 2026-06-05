package com.switchplatform.platform.repository.credit;

import com.switchplatform.platform.model.credit.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {
    List<InstallmentPlan> findByCreditLineId(UUID creditLineId);
    List<InstallmentPlan> findByCreditLineIdAndStatus(UUID creditLineId, InstallmentPlan.InstallmentStatus status);
}
