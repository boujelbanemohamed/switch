package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.MdrPlan;
import com.switchplatform.platform.model.acquiring.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final ConcurrentMap<UUID, Merchant> merchantStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> merchantCodeIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, MdrPlan> mdrPlanStore = new ConcurrentHashMap<>();
    private final AtomicLong mdrPlanIdCounter = new AtomicLong(1);

    @Transactional
    public Merchant onboardMerchant(Merchant merchant) {
        if (merchant.getLegalName() == null || merchant.getLegalName().isBlank()) {
            throw new IllegalArgumentException("legalName is required");
        }
        if (merchant.getMerchantId() == null || merchant.getMerchantId().isBlank()) {
            throw new IllegalArgumentException("merchantId is required");
        }
        if (merchantCodeIndex.containsKey(merchant.getMerchantId())) {
            throw new IllegalArgumentException("merchantId already exists: " + merchant.getMerchantId());
        }
        if (merchant.getId() == null) {
            merchant.setId(UUID.randomUUID());
        }
        merchant.setStatus(Merchant.MerchantStatus.PENDING_ONBOARDING);
        if (merchant.getOnboardingDate() == null) {
            merchant.setOnboardingDate(LocalDate.now());
        }
        merchant.setCreatedAt(OffsetDateTime.now());
        merchant.setUpdatedAt(OffsetDateTime.now());

        merchantStore.put(merchant.getId(), merchant);
        merchantCodeIndex.put(merchant.getMerchantId(), merchant.getId());
        log.info("Onboarded merchant {} ({})", merchant.getId(), merchant.getLegalName());
        return merchant;
    }

    @Transactional
    public Merchant approveMerchant(UUID merchantId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        if (merchant.getStatus() != Merchant.MerchantStatus.PENDING_ONBOARDING) {
            log.warn("Merchant {} is not in PENDING_ONBOARDING state", merchantId);
        }
        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        merchant.setActivationDate(LocalDate.now());
        merchant.setUpdatedAt(OffsetDateTime.now());
        log.info("Approved merchant {}", merchantId);
        return merchant;
    }

    @Transactional
    public Merchant suspendMerchant(UUID merchantId, String reason) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        merchant.setStatus(Merchant.MerchantStatus.SUSPENDED);
        merchant.setUpdatedAt(OffsetDateTime.now());
        log.info("Suspended merchant {} reason: {}", merchantId, reason);
        return merchant;
    }

    @Transactional
    public Merchant terminateMerchant(UUID merchantId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        merchant.setStatus(Merchant.MerchantStatus.TERMINATED);
        merchant.setTerminationDate(LocalDate.now());
        merchant.setUpdatedAt(OffsetDateTime.now());
        log.info("Terminated merchant {}", merchantId);
        return merchant;
    }

    @Transactional(readOnly = true)
    public Optional<Merchant> getMerchant(UUID merchantId) {
        return Optional.ofNullable(merchantStore.get(merchantId));
    }

    @Transactional(readOnly = true)
    public Optional<Merchant> getMerchantByCode(String code) {
        return Optional.ofNullable(merchantCodeIndex.get(code))
                .map(merchantStore::get);
    }

    @Transactional(readOnly = true)
    public List<Merchant> listMerchantsByStatus(String status) {
        return merchantStore.values().stream()
                .filter(m -> m.getStatus().name().equals(status))
                .collect(Collectors.toList());
    }

    @Transactional
    public Merchant updateMerchant(UUID id, Merchant update) {
        Merchant existing = getMerchantOrThrow(id);
        if (update.getLegalName() != null) existing.setLegalName(update.getLegalName());
        if (update.getTradingName() != null) existing.setTradingName(update.getTradingName());
        if (update.getMerchantCategoryCode() != null) existing.setMerchantCategoryCode(update.getMerchantCategoryCode());
        if (update.getRegistrationNumber() != null) existing.setRegistrationNumber(update.getRegistrationNumber());
        if (update.getTaxId() != null) existing.setTaxId(update.getTaxId());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getWebsite() != null) existing.setWebsite(update.getWebsite());
        if (update.getAddressLine1() != null) existing.setAddressLine1(update.getAddressLine1());
        if (update.getAddressLine2() != null) existing.setAddressLine2(update.getAddressLine2());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getPostalCode() != null) existing.setPostalCode(update.getPostalCode());
        if (update.getCountryCode() != null) existing.setCountryCode(update.getCountryCode());
        if (update.getRiskLevel() != null) existing.setRiskLevel(update.getRiskLevel());
        if (update.getSettlementMethod() != null) existing.setSettlementMethod(update.getSettlementMethod());
        if (update.getSettlementCurrency() != null) existing.setSettlementCurrency(update.getSettlementCurrency());
        if (update.getSettlementAccountIban() != null) existing.setSettlementAccountIban(update.getSettlementAccountIban());
        if (update.getSettlementCycle() != null) existing.setSettlementCycle(update.getSettlementCycle());
        if (update.getMdrPercentage() != null) existing.setMdrPercentage(update.getMdrPercentage());
        if (update.getMdrFixedFee() != null) existing.setMdrFixedFee(update.getMdrFixedFee());
        if (update.getMdrPlanId() != null) existing.setMdrPlanId(update.getMdrPlanId());
        existing.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated merchant {}", id);
        return existing;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateMdr(UUID merchantId, BigDecimal amount, String cardBrand, String cardType) {
        Merchant merchant = getMerchantOrThrow(merchantId);

        Optional<MdrPlan> matchingPlan = mdrPlanStore.values().stream()
                .filter(p -> p.getMerchantId() != null && p.getMerchantId().equals(merchantId))
                .filter(p -> p.getCardBrand() != null && p.getCardBrand().equalsIgnoreCase(cardBrand))
                .filter(p -> p.getCardType() == null || p.getCardType().equalsIgnoreCase(cardType))
                .findFirst();

        BigDecimal rate;
        BigDecimal fixedFee;

        if (matchingPlan.isPresent()) {
            MdrPlan plan = matchingPlan.get();
            rate = plan.getDomesticRate();
            fixedFee = plan.getFixedFeeDomestic();
            log.debug("Using MDR plan {} for merchant {}: rate={}%, fixedFee={}",
                    plan.getId(), merchantId, rate, fixedFee);
        } else {
            rate = merchant.getMdrPercentage();
            fixedFee = merchant.getMdrFixedFee() != null ? merchant.getMdrFixedFee() : BigDecimal.ZERO;
            if (rate == null) {
                throw new IllegalStateException("No MDR plan or default rate configured for merchant " + merchantId);
            }
            log.debug("Using default MDR rate for merchant {}: rate={}%, fixedFee={}", merchantId, rate, fixedFee);
        }

        BigDecimal fee = amount.multiply(rate).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP).add(fixedFee);
        log.info("Calculated MDR for merchant {} on amount {}: {}", merchantId, amount, fee);
        return fee;
    }

    void addMdrPlan(MdrPlan plan) {
        if (plan.getId() == null) {
            plan.setId(UUID.randomUUID());
        }
        mdrPlanStore.put(plan.getId(), plan);
    }

    private Merchant getMerchantOrThrow(UUID merchantId) {
        Merchant merchant = merchantStore.get(merchantId);
        if (merchant == null) {
            throw new IllegalArgumentException("Merchant not found: " + merchantId);
        }
        return merchant;
    }
}
