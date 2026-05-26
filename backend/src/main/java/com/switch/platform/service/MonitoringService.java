package com.switch.platform.service;

import com.switch.platform.model.Transaction;
import com.switch.platform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final TransactionRepository transactionRepository;

    public Optional<Transaction> findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    public Optional<Transaction> findByStan(String stan) {
        return transactionRepository.findByStan(stan);
    }

    public Optional<Transaction> findByRrn(String rrn) {
        return transactionRepository.findByRrn(rrn);
    }

    public Page<Transaction> getRecentTransactions(int page, int size) {
        return transactionRepository.findAll(
                org.springframework.data.domain.PageRequest.of(
                        page, size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "requestAt")));
    }

    public Map<String, Object> getDashboardStats() {
        OffsetDateTime lastHour = OffsetDateTime.now().minusHours(1);
        OffsetDateTime last24h = OffsetDateTime.now().minusHours(24);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLastHour",
                transactionRepository.countTransactionsSince(lastHour));
        stats.put("totalLast24h",
                transactionRepository.countTransactionsSince(last24h));
        stats.put("avgProcessingTimeMs",
                transactionRepository.averageProcessingTimeSince(last24h));
        stats.put("statusBreakdown",
                formatStatusBreakdown(
                        transactionRepository.statusBreakdownSince(last24h)));

        Map<String, Long> statusCounts = new HashMap<>();
        for (Transaction.TransactionStatus status : Transaction.TransactionStatus.values()) {
            statusCounts.put(status.name(),
                    transactionRepository.countByStatus(status));
        }
        stats.put("totalByStatus", statusCounts);

        return stats;
    }

    private Map<String, Long> formatStatusBreakdown(List<Object[]> raw) {
        return raw.stream()
                .collect(Collectors.toMap(
                        row -> ((Transaction.TransactionStatus) row[0]).name(),
                        row -> (Long) row[1]));
    }
}
