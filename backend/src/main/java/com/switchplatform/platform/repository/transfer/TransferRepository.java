package com.switchplatform.platform.repository.transfer;

import com.switchplatform.platform.model.transfer.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    List<Transfer> findBySourceAccountIdOrderByCreatedAtDesc(UUID sourceAccountId);
    List<Transfer> findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(UUID accountId1, UUID accountId2);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.sourceAccountId = :acc AND t.status = 'COMPLETED' AND CAST(t.createdAt AS LocalDate) = :date AND t.currencyCode = :ccy")
    BigDecimal sumDailyAmount(@Param("acc") UUID sourceAccountId, @Param("date") LocalDate date, @Param("ccy") String currency);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.sourceAccountId = :acc AND t.status = 'COMPLETED' AND CAST(t.createdAt AS LocalDate) = :date")
    Long countDailyTransfers(@Param("acc") UUID sourceAccountId, @Param("date") LocalDate date);
}
