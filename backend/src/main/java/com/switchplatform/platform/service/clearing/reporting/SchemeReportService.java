package com.switchplatform.platform.service.clearing.reporting;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates quarterly scheme-level reports (volumes, amounts by scheme and transaction type).
 *
 * LAYOUT NOTE: Official Visa and Mastercard quarterly report templates are not publicly available.
 * This implementation produces a generic structured CSV report for internal use.
 * When the official scheme templates (Visa Quarterly Report, Mastercard Settlement Summary)
 * are obtained, replace or extend this service with the proper format.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeReportService {

    private final TransactionRepository transactionRepository;

    public String generateQuarterlyReport(int year, int quarter, String scheme) {
        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);

        OffsetDateTime start = startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = endDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toOffsetDateTime();

        List<Transaction> transactions = transactionRepository
                .findByRequestAtBetweenOrderByRequestAtDesc(start, end, org.springframework.data.domain.PageRequest.of(0, 10000))
                .getContent();

        if (!"ALL".equalsIgnoreCase(scheme)) {
            transactions = transactions.stream()
                    .filter(tx -> {
                        String parsed = tx.getParsedMessage();
                        if (parsed == null) return false;
                        return parsed.toLowerCase().contains(scheme.toLowerCase());
                    })
                    .collect(Collectors.toList());
        }

        Map<String, Summary> byType = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            String type = tx.getTransactionType() != null ? tx.getTransactionType() : "OTHR";
            byType.computeIfAbsent(type, k -> new Summary()).add(tx);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("scheme_report\n");
        sb.append("year,quarter,scheme,generated\n");
        sb.append(year).append(",").append(quarter).append(",")
                .append(scheme).append(",")
                .append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        sb.append("transaction_type,volume,total_amount,avg_amount,currency\n");

        for (var entry : byType.entrySet()) {
            Summary s = entry.getValue();
            sb.append(entry.getKey()).append(",")
                    .append(s.count).append(",")
                    .append(s.total).append(",")
                    .append(s.count > 0 ? s.total.divide(java.math.BigDecimal.valueOf(s.count), java.math.RoundingMode.HALF_UP) : 0).append(",")
                    .append("TND").append("\n");
        }

        sb.append("\nTOTAL,")
                .append(transactions.size()).append(",")
                .append(byType.values().stream().mapToLong(s -> s.total.longValue()).sum()).append(",,")
                .append("TND\n");

        log.info("Quarterly report generated: Q{}/{} scheme={} transactions={}",
                quarter, year, scheme, transactions.size());
        return sb.toString();
    }

    private static class Summary {
        long count = 0;
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        void add(Transaction tx) {
            count++;
            if (tx.getAmount() != null) total = total.add(tx.getAmount());
        }
    }
}
