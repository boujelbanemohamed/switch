package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.InterchangeResult;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClearingService {

    private final Map<UUID, ClearingRecord> clearingRecords = new ConcurrentHashMap<>();
    private final Map<UUID, NettingRecord> nettingRecords = new ConcurrentHashMap<>();
    private final Map<UUID, ReconciliationRecord> reconciliationRecords = new ConcurrentHashMap<>();
    private final AtomicLong reconciliationIdSeq = new AtomicLong(0);

    private final InterchangeService interchangeService;

    @Data
    @Builder
    public static class ClearingData {
        private String transactionId;
        private UUID acquiringParticipantId;
        private UUID issuingParticipantId;
        private BigDecimal amount;
        private String currencyCode;
        private BigDecimal interchangeAmount;
        private BigDecimal feeAmount;
        private String messageType;
        private OffsetDateTime transactionDate;
        private String cardBrand;
        private String cardType;
        private String merchantCategoryCode;
        private String region;
    }

    public ClearingRecord processClearing(ClearingData data) {
        if (data.getAmount() != null && data.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + data.getAmount());
        }
        if (data.getAcquiringParticipantId() == null) {
            throw new IllegalArgumentException("Acquiring participant is required");
        }
        if (data.getIssuingParticipantId() == null) {
            throw new IllegalArgumentException("Issuing participant is required");
        }

        String region = data.getRegion() != null ? data.getRegion() : "TN";
        String brand = data.getCardBrand() != null ? data.getCardBrand() : "VISA";
        String cardType = data.getCardType() != null ? data.getCardType() : "DEBIT";
        String mcc = data.getMerchantCategoryCode();

        BigDecimal interchangeAmount;
        BigDecimal interchangeFee;
        String interchangeBreakdown;

        if (data.getInterchangeAmount() != null) {
            interchangeAmount = data.getInterchangeAmount();
            interchangeFee = interchangeAmount;
            interchangeBreakdown = "Manual override";
        } else {
            InterchangeResult result = interchangeService.calculateInterchange(
                    brand, cardType, region, mcc, data.getAmount(), data.getCurrencyCode());
            interchangeAmount = result.totalFee();
            interchangeFee = result.totalFee();
            interchangeBreakdown = result.breakdown();
        }

        BigDecimal feeAmount = data.getFeeAmount() != null
                ? data.getFeeAmount()
                : interchangeAmount;
        BigDecimal netAmount = data.getAmount().subtract(interchangeAmount);

        ClearingRecord record = ClearingRecord.builder()
                .clearingDate(data.getTransactionDate() != null
                        ? data.getTransactionDate().toLocalDate() : LocalDate.now())
                .transactionId(data.getTransactionId())
                .acquiringParticipantId(data.getAcquiringParticipantId())
                .issuingParticipantId(data.getIssuingParticipantId())
                .amount(data.getAmount())
                .currencyCode(data.getCurrencyCode())
                .interchangeAmount(interchangeAmount)
                .interchangeFee(interchangeFee)
                .interchangeBreakdown(interchangeBreakdown)
                .feeAmount(feeAmount)
                .netAmount(netAmount)
                .messageType(data.getMessageType())
                .transactionDate(data.getTransactionDate())
                .status(ClearingRecord.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        if (record.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        clearingRecords.put(record.getId(), record);
        log.info("Processed clearing record {} for transaction {}",
                record.getId(), data.getTransactionId());
        return record;
    }

    private BigDecimal calculateInterchangeFee(ClearingData data) {
        BigDecimal amount = data.getAmount() != null ? data.getAmount() : BigDecimal.ZERO;
        String brand = data.getCardBrand() != null ? data.getCardBrand().toUpperCase() : "VISA";
        String type = data.getCardType() != null ? data.getCardType().toUpperCase() : "DEBIT";
        String mcc = data.getMerchantCategoryCode();

        BigDecimal rate;
        BigDecimal fixedFee;

        switch (brand) {
            case "VISA":
                if ("CREDIT".equals(type)) {
                    rate = new BigDecimal("0.0120");
                    fixedFee = new BigDecimal("0.50");
                } else if ("PREPAID".equals(type)) {
                    rate = new BigDecimal("0.0080");
                    fixedFee = new BigDecimal("0.20");
                } else {
                    rate = new BigDecimal("0.0095");
                    fixedFee = new BigDecimal("0.25");
                }
                break;
            case "MASTERCARD":
                if ("CREDIT".equals(type)) {
                    rate = new BigDecimal("0.0130");
                    fixedFee = new BigDecimal("0.55");
                } else if ("PREPAID".equals(type)) {
                    rate = new BigDecimal("0.0075");
                    fixedFee = new BigDecimal("0.15");
                } else {
                    rate = new BigDecimal("0.0100");
                    fixedFee = new BigDecimal("0.30");
                }
                break;
            case "CB":
            case "CARTE_BLEUE":
                rate = new BigDecimal("0.0080");
                fixedFee = new BigDecimal("0.15");
                break;
            case "E-DINAR":
                rate = new BigDecimal("0.0050");
                fixedFee = new BigDecimal("0.10");
                break;
            default:
                rate = new BigDecimal("0.0110");
                fixedFee = new BigDecimal("0.35");
        }

        if (mcc != null && ("4814".equals(mcc) || "4899".equals(mcc))) {
            rate = rate.add(new BigDecimal("0.0020"));
        }

        BigDecimal percentageFee = amount.multiply(rate).setScale(3, java.math.RoundingMode.HALF_UP);
        BigDecimal totalFee = percentageFee.add(fixedFee).setScale(3, java.math.RoundingMode.HALF_UP);

        log.info("Interchange fee calculated: brand={}, type={}, amount={}, rate={}, fee={}",
                brand, type, amount, rate, totalFee);
        return totalFee;
    }

    public ClearingRecord clearTransaction(UUID clearingId) {
        ClearingRecord record = clearingRecords.get(clearingId);
        if (record == null) {
            throw new IllegalArgumentException("Clearing record not found: " + clearingId);
        }
        if (record.getStatus() != ClearingRecord.Status.PENDING) {
            throw new IllegalStateException(
                    "Clearing record " + clearingId + " is in state " + record.getStatus());
        }
        record.setStatus(ClearingRecord.Status.CLEARED);
        log.info("Clearing record {} cleared", clearingId);
        return record;
    }

    public ClearingRecord disputeClearing(UUID clearingId, String reason) {
        ClearingRecord record = clearingRecords.get(clearingId);
        if (record == null) {
            throw new IllegalArgumentException("Clearing record not found: " + clearingId);
        }
        record.setStatus(ClearingRecord.Status.DISPUTED);
        record.setDisputeReason(reason);
        log.info("Clearing record {} disputed: {}", clearingId, reason);
        return record;
    }

    public List<ClearingRecord> getClearingByDate(LocalDate date) {
        return clearingRecords.values().stream()
                .filter(r -> r.getClearingDate().equals(date))
                .collect(Collectors.toList());
    }

    public List<ClearingRecord> getClearingByParticipant(UUID participantId) {
        return clearingRecords.values().stream()
                .filter(r -> r.getAcquiringParticipantId().equals(participantId)
                        || r.getIssuingParticipantId().equals(participantId))
                .collect(Collectors.toList());
    }

    public List<NettingRecord> calculateNetting(LocalDate date) {
        List<ClearingRecord> cleared = clearingRecords.values().stream()
                .filter(r -> r.getClearingDate().equals(date)
                        && r.getStatus() == ClearingRecord.Status.CLEARED)
                .collect(Collectors.toList());

        Map<String, List<ClearingRecord>> grouped = cleared.stream()
                .collect(Collectors.groupingBy(r -> {
                    UUID a = r.getAcquiringParticipantId();
                    UUID i = r.getIssuingParticipantId();
                    return a.compareTo(i) < 0 ? a + ":" + i : i + ":" + a;
                }));

        List<NettingRecord> results = new ArrayList<>();

        for (Map.Entry<String, List<ClearingRecord>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split(":");
            UUID p1 = UUID.fromString(parts[0]);
            UUID p2 = UUID.fromString(parts[1]);
            List<ClearingRecord> records = entry.getValue();

            BigDecimal p1ToP2 = BigDecimal.ZERO;
            BigDecimal p2ToP1 = BigDecimal.ZERO;
            int count = records.size();

            for (ClearingRecord r : records) {
                if (r.getAcquiringParticipantId().equals(p1)
                        && r.getIssuingParticipantId().equals(p2)) {
                    p1ToP2 = p1ToP2.add(r.getNetAmount() != null ? r.getNetAmount() : BigDecimal.ZERO);
                } else {
                    p2ToP1 = p2ToP1.add(r.getNetAmount() != null ? r.getNetAmount() : BigDecimal.ZERO);
                }
            }

            BigDecimal netAmount = p1ToP2.subtract(p2ToP1);

            NettingRecord netting = NettingRecord.builder()
                    .nettingDate(date)
                    .participantId(p1)
                    .counterpartyId(p2)
                    .totalSent(p1ToP2)
                    .totalReceived(p2ToP1)
                    .netAmount(netAmount)
                    .currencyCode(records.get(0).getCurrencyCode())
                    .transactionCount(count)
                    .status(NettingRecord.Status.PENDING)
                    .createdAt(OffsetDateTime.now())
                    .build();

            if (netting.getId() == null) {
                netting.setId(UUID.randomUUID());
            }
            nettingRecords.put(netting.getId(), netting);
            results.add(netting);
        }

        log.info("Calculated netting for {} participant pairs on {}", results.size(), date);
        return results;
    }

    public NettingRecord confirmNetting(UUID nettingId) {
        NettingRecord record = nettingRecords.get(nettingId);
        if (record == null) {
            throw new IllegalArgumentException("Netting record not found: " + nettingId);
        }
        record.setStatus(NettingRecord.Status.CONFIRMED);
        log.info("Netting record {} confirmed", nettingId);
        return record;
    }

    public NettingRecord settleNetting(UUID nettingId, String reference) {
        NettingRecord record = nettingRecords.get(nettingId);
        if (record == null) {
            throw new IllegalArgumentException("Netting record not found: " + nettingId);
        }
        record.setStatus(NettingRecord.Status.SETTLED);
        record.setSettlementReference(reference);
        record.setSettledAt(OffsetDateTime.now());
        log.info("Netting record {} settled: {}", nettingId, reference);
        return record;
    }

    public ReconciliationRecord getReconciliationReport(LocalDate date, UUID participantId) {
        Optional<ReconciliationRecord> existing = reconciliationRecords.values().stream()
                .filter(r -> r.getReconciliationDate().equals(date)
                        && r.getParticipantId().equals(participantId)
                        && r.getSource() == ReconciliationRecord.Source.SWITCH)
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        List<ClearingRecord> records = clearingRecords.values().stream()
                .filter(r -> r.getClearingDate().equals(date)
                        && (r.getAcquiringParticipantId().equals(participantId)
                        || r.getIssuingParticipantId().equals(participantId)))
                .collect(Collectors.toList());

        int totalCount = records.size();
        BigDecimal totalAmount = records.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = records.stream()
                .map(r -> r.getFeeAmount() != null ? r.getFeeAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReconciliationRecord report = ReconciliationRecord.builder()
                .reconciliationDate(date)
                .participantId(participantId)
                .source(ReconciliationRecord.Source.SWITCH)
                .totalTransactions(totalCount)
                .totalAmount(totalAmount)
                .totalFees(totalFees)
                .matchedCount(totalCount)
                .unmatchedCount(0)
                .discrepancyCount(0)
                .status(ReconciliationRecord.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        if (report.getId() == null) {
            report.setId(UUID.randomUUID());
        }
        reconciliationRecords.put(report.getId(), report);
        log.info("Created reconciliation report for participant {} on {}", participantId, date);
        return report;
    }
}
