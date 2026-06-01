package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpgTransactionRepository extends JpaRepository<EpgTransaction, UUID> {

    Optional<EpgTransaction> findByMerchantTransactionId(String merchantTransactionId);

    Optional<EpgTransaction> findByMerchantIdAndMerchantTransactionId(UUID merchantId, String merchantTransactionId);

    List<EpgTransaction> findByMerchantId(UUID merchantId);

    Page<EpgTransaction> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<EpgTransaction> findByStatus(EpgTransaction.Status status);

    Page<EpgTransaction> findByStatusOrderByCreatedAtDesc(EpgTransaction.Status status, Pageable pageable);

    Page<EpgTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    List<EpgTransaction> findByMerchantIdAndCreatedAtBetween(
            UUID merchantId, OffsetDateTime from, OffsetDateTime to);
}
