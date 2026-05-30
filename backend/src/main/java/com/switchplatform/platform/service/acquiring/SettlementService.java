package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.NettingResult;
import com.switchplatform.platform.model.acquiring.SettlementRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final ConcurrentMap<UUID, SettlementRecord> settlementStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<UUID>> merchantSettlementIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<TransactionEntry>> transactionStore = new ConcurrentHashMap<>();
    private final MerchantService merchantService;

    record TransactionEntry(BigDecimal amount, String cardBrand, String cardType, OffsetDateTime timestamp) {}

    public void recordTransaction(String merchantId, BigDecimal amount, String cardBrand, String cardType) {
        transactionStore.computeIfAbsent(merchantId, k -> new CopyOnWriteArrayList<>())
                .add(new TransactionEntry(amount, cardBrand, cardType, OffsetDateTime.now()));
        log.debug("Recorded transaction for merchant {}: amount={}", merchantId, amount);
    }

    public SettlementRecord createSettlement(String merchantId, LocalDate settlementDate, String currencyCode) {
        Merchant merchant = merchantService.getMerchantByCode(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        List<TransactionEntry> txns = transactionStore.getOrDefault(merchantId, List.of());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;

        for (TransactionEntry txn : txns) {
            totalAmount = totalAmount.add(txn.amount());
            try {
                BigDecimal fee = merchantService.calculateMdr(merchant.getId(), txn.amount(), txn.cardBrand(), txn.cardType());
                totalFee = totalFee.add(fee);
            } catch (Exception e) {
                log.warn("Could not calculate MDR for merchant {}: {}", merchantId, e.getMessage());
            }
        }

        BigDecimal netAmount = totalAmount.subtract(totalFee);

        SettlementRecord record = SettlementRecord.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .settlementDate(settlementDate)
                .totalAmount(totalAmount)
                .totalFee(totalFee)
                .netAmount(netAmount)
                .currencyCode(currencyCode)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();

        settlementStore.put(record.getId(), record);
        merchantSettlementIndex.computeIfAbsent(merchantId, k -> new CopyOnWriteArrayList<>()).add(record.getId());

        transactionStore.remove(merchantId);

        log.info("Created settlement {} for merchant {}: amount={}, fee={}, net={}",
                record.getId(), merchantId, totalAmount, totalFee, netAmount);
        return record;
    }

    public SettlementRecord confirmSettlement(UUID settlementId) {
        SettlementRecord record = getSettlementOrThrow(settlementId);
        if (!"PENDING".equals(record.getStatus())) {
            throw new IllegalStateException("Settlement " + settlementId + " is not in PENDING state");
        }
        record.setStatus("CONFIRMED");
        record.setPaymentRef("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setConfirmedAt(OffsetDateTime.now());
        log.info("Confirmed settlement {} with ref {}", settlementId, record.getPaymentRef());
        return record;
    }

    public SettlementRecord getSettlement(UUID settlementId) {
        return getSettlementOrThrow(settlementId);
    }

    public List<SettlementRecord> getSettlementsByMerchant(String merchantId, LocalDate from, LocalDate to) {
        List<UUID> ids = merchantSettlementIndex.getOrDefault(merchantId, List.of());
        return ids.stream()
                .map(settlementStore::get)
                .filter(Objects::nonNull)
                .filter(s -> !s.getSettlementDate().isBefore(from))
                .filter(s -> !s.getSettlementDate().isAfter(to))
                .collect(Collectors.toList());
    }

    public NettingResult calculateMerchantNetting(String merchantId, LocalDate date) {
        List<SettlementRecord> settlements = getSettlementsByMerchant(merchantId, date, date);

        BigDecimal grossAmount = settlements.stream()
                .map(SettlementRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = settlements.stream()
                .map(SettlementRecord::getTotalFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netAmount = settlements.stream()
                .map(SettlementRecord::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = settlements.size();
        String currency = settlements.isEmpty() ? "TND" : settlements.get(0).getCurrencyCode();

        return new NettingResult(merchantId, date, grossAmount, totalFees, netAmount, count, currency);
    }

    private SettlementRecord getSettlementOrThrow(UUID id) {
        SettlementRecord record = settlementStore.get(id);
        if (record == null) {
            throw new IllegalArgumentException("Settlement not found: " + id);
        }
        return record;
    }
}
