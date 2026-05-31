package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, UUID> {
    List<MerchantSettlement> findByMerchantId(UUID merchantId);
    List<MerchantSettlement> findBySettlementDate(LocalDate date);
    List<MerchantSettlement> findByMerchantIdAndSettlementDate(UUID merchantId, LocalDate date);
}
