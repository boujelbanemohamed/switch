package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.repository.acquiring.MerchantSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantSettlementService {

    private final MerchantSettlementRepository merchantSettlementRepository;

    @Transactional
    public MerchantSettlement createSettlement(UUID merchantId, LocalDate date, String currency) {
        MerchantSettlement settlement = MerchantSettlement.builder()
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

        merchantSettlementRepository.save(settlement);
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
        return merchantSettlementRepository.save(settlement);
    }

    @Transactional
    public MerchantSettlement confirmSettlement(UUID settlementId) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        settlement.setStatus(MerchantSettlement.SettlementStatus.CONFIRMED);
        log.info("Confirmed settlement {}", settlementId);
        return merchantSettlementRepository.save(settlement);
    }

    @Transactional
    public MerchantSettlement markPaid(UUID settlementId, String paymentRef) {
        MerchantSettlement settlement = getSettlementOrThrow(settlementId);
        settlement.setStatus(MerchantSettlement.SettlementStatus.PAID);
        settlement.setPaymentReference(paymentRef);
        settlement.setPaidAt(OffsetDateTime.now());
        log.info("Marked settlement {} as paid (ref: {})", settlementId, paymentRef);
        return merchantSettlementRepository.save(settlement);
    }

    @Transactional(readOnly = true)
    public Optional<MerchantSettlement> getSettlement(UUID id) {
        return merchantSettlementRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MerchantSettlement> getMerchantSettlements(UUID merchantId, LocalDate from, LocalDate to) {
        return merchantSettlementRepository.findByMerchantId(merchantId).stream()
                .filter(s -> !s.getSettlementDate().isBefore(from))
                .filter(s -> !s.getSettlementDate().isAfter(to))
                .toList();
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
        return merchantSettlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + id));
    }
}
