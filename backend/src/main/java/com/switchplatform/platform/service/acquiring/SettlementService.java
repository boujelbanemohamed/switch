package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.NettingResult;
import com.switchplatform.platform.model.acquiring.SettlementRecord;
import com.switchplatform.platform.repository.acquiring.SettlementRecordRepository;
import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.event.SettlementInitiatedEvent;
import com.switchplatform.platform.event.SettlementCompletedEvent;
import com.switchplatform.platform.service.ledger.LedgerPostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SettlementRecordRepository settlementRecordRepository;
    private final ConcurrentMap<String, List<TransactionEntry>> transactionStore = new ConcurrentHashMap<>();
    private final MerchantService merchantService;
    private final LedgerPostingEngine ledgerPostingEngine;
    private final EventPublisher eventPublisher;

    record TransactionEntry(BigDecimal amount, String cardBrand, String cardType, OffsetDateTime timestamp) {}

    public void recordTransaction(String merchantId, BigDecimal amount, String cardBrand, String cardType) {
        transactionStore.computeIfAbsent(merchantId, k -> new CopyOnWriteArrayList<>())
                .add(new TransactionEntry(amount, cardBrand, cardType, OffsetDateTime.now()));
        log.debug("Recorded transaction for merchant {}: amount={}", merchantId, amount);
    }

    public void recordTransactionForSettlement(String merchantId, BigDecimal amount, String cardBrand, String cardType) {
        recordTransaction(merchantId, amount, cardBrand, cardType);
        log.info("Transaction recorded for settlement: merchantId={}, amount={}, brand={}, type={}",
                merchantId, amount, cardBrand, cardType);
    }

    @Transactional
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
                .merchantId(merchant.getId())
                .settlementDate(settlementDate)
                .totalAmount(totalAmount)
                .totalFee(totalFee)
                .netAmount(netAmount)
                .currencyCode(currencyCode)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();

        record = settlementRecordRepository.save(record);

        transactionStore.remove(merchantId);

        log.info("Created settlement {} for merchant {}: amount={}, fee={}, net={}",
                record.getId(), merchantId, totalAmount, totalFee, netAmount);

        eventPublisher.publishSettlementInitiated(new SettlementInitiatedEvent(
                record.getId(), null, merchantId,
                totalAmount.toPlainString(), currencyCode, merchantId, OffsetDateTime.now()));

        return record;
    }

    @Transactional
    public SettlementRecord confirmSettlement(UUID settlementId) {
        SettlementRecord record = getSettlementOrThrow(settlementId);
        if (!"PENDING".equals(record.getStatus())) {
            throw new IllegalStateException("Settlement " + settlementId + " is not in PENDING state");
        }
        record.setStatus("CONFIRMED");
        record.setPaymentRef("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setConfirmedAt(OffsetDateTime.now());
        record = settlementRecordRepository.save(record);

        try {
            String txnRef = record.getPaymentRef();
            ledgerPostingEngine.postSettlement(
                    txnRef, record.getMerchantId().toString(),
                    record.getNetAmount(), record.getTotalFee(),
                    BigDecimal.ZERO, record.getCurrencyCode());
        } catch (Exception e) {
            log.warn("Ledger posting failed for settlement {}: {}", settlementId, e.getMessage());
        }

        log.info("Confirmed settlement {} with ref {}", settlementId, record.getPaymentRef());

        eventPublisher.publishSettlementCompleted(new SettlementCompletedEvent(
                settlementId, null, record.getMerchantId().toString(),
                record.getNetAmount().toPlainString(), record.getCurrencyCode(),
                record.getMerchantId().toString(), "CONFIRMED", OffsetDateTime.now()));

        return record;
    }

    @Transactional(readOnly = true)
    public SettlementRecord getSettlement(UUID settlementId) {
        return getSettlementOrThrow(settlementId);
    }

    @Transactional(readOnly = true)
    public List<SettlementRecord> getSettlementsByMerchant(UUID merchantId, LocalDate from, LocalDate to) {
        return settlementRecordRepository.findByMerchantIdAndSettlementDateBetween(merchantId, from, to);
    }

    @Transactional(readOnly = true)
    public NettingResult calculateMerchantNetting(UUID merchantId, LocalDate date) {
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

        return NettingResult.builder()
                .merchantId(merchantId)
                .date(date)
                .grossAmount(grossAmount)
                .totalFees(totalFees)
                .netAmount(netAmount)
                .transactionCount(count)
                .currencyCode(currency)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private SettlementRecord getSettlementOrThrow(UUID id) {
        return settlementRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + id));
    }
}
