package com.switchplatform.platform.repository.fx;

import com.switchplatform.platform.model.fx.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FxRateRepository extends JpaRepository<FxRate, UUID> {
    Optional<FxRate> findTopBySourceCurrencyAndTargetCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String source, String target, LocalDate date);
}
