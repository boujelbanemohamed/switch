package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.NettingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NettingResultRepository extends JpaRepository<NettingResult, UUID> {
    List<NettingResult> findByMerchantId(UUID merchantId);
    List<NettingResult> findByDate(LocalDate date);
}
