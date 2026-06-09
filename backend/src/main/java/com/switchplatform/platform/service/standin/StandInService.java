package com.switchplatform.platform.service.standin;

import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.event.StandInUsedEvent;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.standin.StandInAuthorization;
import com.switchplatform.platform.model.standin.StandInRule;
import com.switchplatform.platform.repository.standin.StandInAuthorizationRepository;
import com.switchplatform.platform.repository.standin.StandInRuleRepository;
import com.switchplatform.platform.service.ParticipantService;
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
public class StandInService {

    private final StandInRuleRepository ruleRepository;
    private final StandInAuthorizationRepository authorizationRepository;
    private final ParticipantService participantService;
    private final EventPublisher eventPublisher;

    @Transactional
    public StandInAuthorization attemptStandIn(String transactionId, UUID issuerId,
                                                String cardBrand, String cardSuffix,
                                                BigDecimal amount, String currency, String mcc) {
        StandInRule rule = findRule(issuerId, cardBrand);

        if (rule == null) {
            log.warn("No stand-in rule for issuer={}, brand={}, decline_if_no_rule=true by default", issuerId, cardBrand);
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "NO_RULE");
        }

        if (!rule.getEnabled()) {
            log.warn("Stand-in rule disabled for issuer={}, brand={}", issuerId, cardBrand);
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "RULE_DISABLED");
        }

        if (amount.compareTo(rule.getMaxAmount()) > 0) {
            log.warn("Stand-in amount {} exceeds maxAmount {} for issuer={}", amount, rule.getMaxAmount(), issuerId);
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "EXCEEDS_MAX_AMOUNT");
        }

        if (!isMccAllowed(mcc, rule.getAllowedMcc())) {
            log.warn("MCC {} not allowed by stand-in rule for issuer={}", mcc, issuerId);
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "MCC_NOT_ALLOWED");
        }

        LocalDate today = LocalDate.now();
        long dailyCount = authorizationRepository.countApprovedForIssuerOnDate(issuerId, today);
        BigDecimal dailySum = authorizationRepository.sumApprovedAmountForIssuerOnDate(issuerId, today);

        if (dailyCount >= rule.getDailyCountLimit()) {
            log.warn("Stand-in daily count limit reached for issuer={}: {} >= {}", issuerId, dailyCount, rule.getDailyCountLimit());
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "DAILY_COUNT_LIMIT");
        }

        if (dailySum.add(amount).compareTo(rule.getDailyAmountLimit()) > 0) {
            log.warn("Stand-in daily amount limit would be exceeded for issuer={}: {} + {} > {}",
                    issuerId, dailySum, amount, rule.getDailyAmountLimit());
            return decline(transactionId, issuerId, cardSuffix, amount, currency, "DAILY_AMOUNT_LIMIT");
        }

        StandInAuthorization auth = StandInAuthorization.builder()
                .transactionId(transactionId)
                .cardSuffix(cardSuffix)
                .issuerParticipantId(issuerId)
                .amount(amount)
                .currencyCode(currency)
                .decision(StandInAuthorization.Decision.APPROVED)
                .reason("STAND_IN_APPROVED")
                .reconciled(false)
                .authorizedAt(OffsetDateTime.now())
                .build();
        auth = authorizationRepository.save(auth);

        eventPublisher.publishStandInUsed(new StandInUsedEvent(
                auth.getId(), transactionId, issuerId, amount, currency, "APPROVED", "STAND_IN_APPROVED", OffsetDateTime.now()));

        log.info("Stand-in APPROVED for transaction={}, issuer={}, amount={} {}",
                transactionId, issuerId, amount, currency);
        return auth;
    }

    private StandInAuthorization decline(String transactionId, UUID issuerId,
                                          String cardSuffix, BigDecimal amount,
                                          String currency, String reason) {
        StandInAuthorization auth = StandInAuthorization.builder()
                .transactionId(transactionId)
                .cardSuffix(cardSuffix)
                .issuerParticipantId(issuerId)
                .amount(amount)
                .currencyCode(currency)
                .decision(StandInAuthorization.Decision.DECLINED)
                .reason(reason)
                .reconciled(false)
                .authorizedAt(OffsetDateTime.now())
                .build();
        auth = authorizationRepository.save(auth);

        eventPublisher.publishStandInUsed(new StandInUsedEvent(
                auth.getId(), transactionId, issuerId, amount, currency, "DECLINED", reason, OffsetDateTime.now()));

        log.info("Stand-in DECLINED for transaction={}, reason={}", transactionId, reason);
        return auth;
    }

    private StandInRule findRule(UUID issuerId, String cardBrand) {
        if (issuerId != null) {
            Optional<StandInRule> specific = ruleRepository
                    .findByIssuerParticipantIdAndCardBrandAndEnabledTrue(issuerId, cardBrand);
            if (specific.isPresent()) return specific.get();

            Optional<StandInRule> brandAll = ruleRepository
                    .findByIssuerParticipantIdAndCardBrandAndEnabledTrue(issuerId, "ALL");
            if (brandAll.isPresent()) return brandAll.get();
        }

        List<StandInRule> global = ruleRepository
                .findByIssuerParticipantIdIsNullAndCardBrandAndEnabledTrue("ALL");
        return global.isEmpty() ? null : global.get(0);
    }

    private boolean isMccAllowed(String mcc, String allowedMcc) {
        if (allowedMcc == null || "*".equals(allowedMcc)) return true;
        if (mcc == null) return false;
        for (String a : allowedMcc.split(",")) {
            if (a.trim().equals(mcc)) return true;
        }
        return false;
    }

    @Transactional
    public void reconcilePending(UUID issuerParticipantId) {
        List<StandInAuthorization> pending = authorizationRepository
                .findByIssuerParticipantIdAndReconciledFalse(issuerParticipantId);
        for (StandInAuthorization auth : pending) {
            auth.setReconciled(true);
            authorizationRepository.save(auth);
            log.info("Stand-in authorization {} reconciled (transaction={})", auth.getId(), auth.getTransactionId());
        }
    }

    public List<StandInRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public StandInRule createRule(StandInRule rule) {
        return ruleRepository.save(rule);
    }

    public StandInRule updateRule(UUID id, StandInRule updated) {
        StandInRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("StandInRule not found: " + id));
        rule.setIssuerParticipantId(updated.getIssuerParticipantId());
        rule.setCardBrand(updated.getCardBrand());
        rule.setEnabled(updated.getEnabled());
        rule.setMaxAmount(updated.getMaxAmount());
        rule.setDailyCountLimit(updated.getDailyCountLimit());
        rule.setDailyAmountLimit(updated.getDailyAmountLimit());
        rule.setAllowedMcc(updated.getAllowedMcc());
        rule.setDeclineIfNoRule(updated.getDeclineIfNoRule());
        return ruleRepository.save(rule);
    }

    public void deleteRule(UUID id) {
        ruleRepository.deleteById(id);
    }

    public List<StandInAuthorization> getAuthorizations(UUID issuerId) {
        if (issuerId != null) {
            return authorizationRepository.findByIssuerParticipantIdAndReconciledFalse(issuerId);
        }
        return authorizationRepository.findByReconciledFalse();
    }

    public long countPending() {
        return authorizationRepository.findByReconciledFalse().size();
    }
}
