package com.switch.platform.repository;

import com.switch.platform.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByStan(String stan);

    Optional<Transaction> findByRrn(String rrn);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    Page<Transaction> findByStatusOrderByRequestAtDesc(
            Transaction.TransactionStatus status, Pageable pageable);

    Page<Transaction> findByRequestAtBetweenOrderByRequestAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    long countByStatus(Transaction.TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.requestAt >= :since")
    long countTransactionsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(AVG(t.processingTimeMs), 0) FROM Transaction t " +
           "WHERE t.status = 'COMPLETED' AND t.requestAt >= :since")
    double averageProcessingTimeSince(@Param("since") OffsetDateTime since);

    @Query("SELECT t.status, COUNT(t) FROM Transaction t " +
           "WHERE t.requestAt >= :since GROUP BY t.status")
    List<Object[]> statusBreakdownSince(@Param("since") OffsetDateTime since);
}
