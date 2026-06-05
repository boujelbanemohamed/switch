package com.switchplatform.platform.service;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.repository.TransactionRepository;
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

    public Page<Transaction> getRecentTransactions(int page, int size,
                                                    String channel, String transactionType, String posEntryMode) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "requestAt"));
        if (channel != null && transactionType != null) {
            return transactionRepository.findByChannelAndTransactionType(channel, transactionType, pageable);
        }
        if (channel != null) {
            return transactionRepository.findByChannel(channel, pageable);
        }
        if (transactionType != null) {
            return transactionRepository.findByTransactionType(transactionType, pageable);
        }
        if (posEntryMode != null) {
            return transactionRepository.findByPosEntryMode(posEntryMode, pageable);
        }
        return transactionRepository.findAll(pageable);
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
