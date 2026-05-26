package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.MerchantSettlement;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantSettlementService {

    private final ConcurrentMap<UUID, MerchantSettlement> settlementStore = new ConcurrentHashMap<>();

    @Transactional
    public MerchantSettlement createSettlement(UUID merchantId, LocalDate date, String currency) {
        MerchantSettlement settlement = MerchantSettlement.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .settlementDate(date)
                .currencyCode(currency)
                .totalTransactions(0)
                .totalAmount(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .totalCommission(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .status(MerchantSettlement.SettlementStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        settlementStore.put(settlement.getId(), settlement);
        log.info("Created settlement {} for merchant {} on {}", settlement.getId(), merchantId, date);
        return settlement;
    }

    @Transactional
    public MerchantSettlement addTransaction(UUID settlementId, BigDecimal amount, BigDecimal fee, BigDecimal commission) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        settlement.setTotalTransactions(settlement.getTotalTransactions() + 1);
        settlement.setTotalAmount(settlement.getTotalAmount().add(amount));
        settlement.setTotalFees(settlement.getTotalFees().add(fee));
        settlement.setTotalCommission(settlement.getTotalCommission().add(commission));
        log.debug("Added transaction to settlement {}: amount={}, fee={}, commission={}",
                settlementId, amount, fee, commission);
        return settlement;
    }

    @Transactional
    public MerchantSettlement confirmSettlement(UUID settlementId) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        settlement.setStatus(MerchantSettlement.SettlementStatus.CONFIRMED);
        log.info("Confirmed settlement {}", settlementId);
        return settlement;
    }

    @Transactional
    public MerchantSettlement markPaid(UUID settlementId, String paymentRef) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        settlement.setStatus(MerchantSettlement.SettlementStatus.PAID);
        settlement.setPaymentReference(paymentRef);
        settlement.setPaidAt(OffsetDateTime.now());
        log.info("Marked settlement {} as paid (ref: {})", settlementId, paymentRef);
        return settlement;
    }

    @Transactional(readOnly = true)
    public Optional<MerchantSettlement> getSettlement(UUID id) {
        return Optional.ofNullable(settlementStore.get(id));
    }

    @Transactional(readOnly = true)
    public List<MerchantSettlement> getMerchantSettlements(UUID merchantId, LocalDate from, LocalDate to) {
        return settlementStore.values().stream()
                .filter(s -> s.getMerchantId().equals(merchantId))
                .filter(s -> !s.getSettlementDate().isBefore(from))
                .filter(s -> !s.getSettlementDate().isAfter(to))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateNetAmount(UUID settlementId) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        BigDecimal net = settlement.getTotalAmount()
                .subtract(settlement.getTotalFees())
                .subtract(settlement.getTotalCommission());
        log.debug("Calculated net amount for settlement {}: {}", settlementId, net);
        return net;
    }

    private MerchantSettlement getSettlementOrThrow(UUID id) {
        MerchantSettlement settlement = settlementStore.get(id);
        if (settlement == null) {
            throw new IllegalArgumentException("Settlement not found: " + id);
        }
        return settlement;
    }
}
