package com.switchplatform.platform.service.authorization;

import com.switchplatform.platform.model.authorization.AuthDecision;
import com.switchplatform.platform.model.authorization.AuthRule;
import com.switchplatform.platform.model.authorization.HoldRecord;
import com.switchplatform.platform.model.authorization.AuthRule.ActionType;
import com.switchplatform.platform.model.authorization.AuthRule.RuleStatus;
import com.switchplatform.platform.model.authorization.CardLimitUsage;
import com.switchplatform.platform.model.authorization.VelocityCheck;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.service.fraud.FraudEngine;
import com.switchplatform.platform.service.issuing.CardAccountService;
import com.switchplatform.platform.service.issuing.CardService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationEngine {

    private final Map<UUID, AuthRule> rules = new ConcurrentHashMap<>();
    private final List<AuthDecision> decisions = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<CardLimitUsage>> cardLimits = new ConcurrentHashMap<>();
    private final Map<UUID, List<VelocityCheck>> velocityChecks = new ConcurrentHashMap<>();

    private final FraudEngine fraudEngine;
    private final CardAccountService cardAccountService;
    private final CardService cardService;
    private final HoldService holdService;

    public AuthorizationResponse authorize(AuthorizationRequest request) {
        long start = System.currentTimeMillis();
        log.info("Authorization request: cardId={}, amount={}, currency={}",
                request.getCardId(), request.getAmount(), request.getCurrencyCode());

        AuthDecision decision = AuthDecision.builder()
                .cardId(request.getCardId())
                .cardholderId(request.getCardholderId())
                .merchantId(request.getMerchantId())
                .terminalId(request.getTerminalId())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .mti(request.getMti())
                .stan(request.getStan())
                .panHash(request.getPanHash())
                .requestedAt(OffsetDateTime.now())
                .build();

        // 1. Evaluate fraud score
        FraudEngine.FraudEvaluationResult fraudResult = evaluateFraud(request);
        decision.setFraudScore(fraudResult.getScore());
        decision.setRiskScore(fraudResult.getScore());

        if (fraudResult.getAction() == FraudEngine.FraudAction.BLOCK) {
            decision.setDecision(AuthDecision.Decision.DECLINED);
            decision.setResponseReason("Blocked by fraud engine (score: " + fraudResult.getScore() + ")");
            decision.setResponseCode("59");
        }

        // 2. Check card account balance
        if (decision.getDecision() == null && request.getCardholderId() != null) {
            List<CardAccount> cardAccounts = cardAccountService.getAccountsByCardholderId(request.getCardholderId());
            if (!cardAccounts.isEmpty()) {
                CardAccount account = cardAccounts.get(0);
                decision.setCardBalanceBefore(account.getAvailableBalance());
                if (request.getAmount() != null
                        && account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                    decision.setDecision(AuthDecision.Decision.DECLINED);
                    decision.setResponseReason("Insufficient balance");
                    decision.setResponseCode("51");
                    log.warn("Insufficient balance: account={}, available={}, required={}",
                            account.getId(), account.getAvailableBalance(), request.getAmount());
                }
            }
        }

        // 2a. Check card-specific account balance via cardAccountId
        if (decision.getDecision() == null && request.getCardId() != null) {
            Optional<Card> cardOpt = cardService.getCard(request.getCardId());
            if (cardOpt.isPresent()) {
                UUID cardAccountId = cardOpt.get().getCardAccountId();
                if (cardAccountId != null) {
                    cardAccountService.getAccount(cardAccountId).ifPresent(account -> {
                        decision.setCardBalanceBefore(account.getAvailableBalance());
                        if (request.getAmount() != null
                                && account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                            decision.setDecision(AuthDecision.Decision.DECLINED);
                            decision.setResponseReason("Insufficient balance (card account)");
                            decision.setResponseCode("51");
                            log.warn("Insufficient balance via cardAccountId: account={}, available={}, required={}",
                                    account.getId(), account.getAvailableBalance(), request.getAmount());
                        }
                    });
                }
            }
        }

        // 3. Evaluate authorization rules
        AuthRule matchedRule = null;
        if (decision.getDecision() == null) {
            List<AuthRule> applicableRules = rules.values().stream()
                    .filter(r -> r.getStatus() == RuleStatus.ACTIVE)
                    .filter(r -> evaluateCondition(r, request))
                    .sorted(Comparator.comparingInt(AuthRule::getPriority))
                    .toList();

            for (AuthRule rule : applicableRules) {
                matchedRule = rule;
                ActionType action = rule.getAction();
                decision.setRuleId(rule.getId());
                decision.setRuleName(rule.getName());
                decision.setResponseCode(rule.getResponseCode());

                if (action == ActionType.DECLINE) {
                    decision.setDecision(AuthDecision.Decision.DECLINED);
                    decision.setResponseReason("Declined by rule: " + rule.getName());
                    break;
                } else if (action == ActionType.CHALLENGE) {
                    decision.setDecision(AuthDecision.Decision.CHALLENGED);
                    decision.setResponseReason("Challenge by rule: " + rule.getName());
                    break;
                } else if (action == ActionType.APPROVE) {
                    decision.setDecision(AuthDecision.Decision.APPROVED);
                    decision.setResponseReason("Approved by rule: " + rule.getName());
                }
            }
        }

        // 4. Check card limits
        if (decision.getDecision() == AuthDecision.Decision.APPROVED
                || decision.getDecision() == null) {
            String limitReason = checkCardLimits(request);
            if (limitReason != null) {
                decision.setDecision(AuthDecision.Decision.DECLINED);
                decision.setResponseReason(limitReason);
                decision.setResponseCode("51");
            }
        }

        // 5. Check velocity
        if (decision.getDecision() == AuthDecision.Decision.APPROVED) {
            String velocityReason = checkVelocity(request);
            if (velocityReason != null) {
                decision.setDecision(AuthDecision.Decision.DECLINED);
                decision.setResponseReason(velocityReason);
                decision.setResponseCode("61");
            }
        }

        // 6. Default approval
        if (decision.getDecision() == null) {
            decision.setDecision(AuthDecision.Decision.APPROVED);
            decision.setResponseCode("00");
            decision.setResponseReason("Approved");
        }

        // 7. Post-approval actions: hold on account + update limit usage
        if (decision.getDecision() == AuthDecision.Decision.APPROVED) {
            if (!postApprovalActions(request, decision)) {
                decision.setDecision(AuthDecision.Decision.DECLINED);
                decision.setResponseCode("51");
                decision.setResponseReason("HOLD_FAILED");
            }
        }

        if (matchedRule != null) {
            int current = matchedRule.getSuccessCount() != null ? matchedRule.getSuccessCount() : 0;
            matchedRule.setSuccessCount(current + 1);
        }

        long elapsed = System.currentTimeMillis() - start;
        decision.setProcessingTimeMs((int) elapsed);
        decision.setDecidedAt(OffsetDateTime.now());
        decisions.add(decision);

        log.info("Authorization result: {} reason={} fraudScore={} time={}ms",
                decision.getDecision(), decision.getResponseReason(),
                decision.getFraudScore(), elapsed);

        return AuthorizationResponse.builder()
                .decision(mapDecision(decision.getDecision()))
                .responseCode(decision.getResponseCode() != null
                        ? decision.getResponseCode() : "00")
                .reason(decision.getResponseReason())
                .processingTimeMs((int) elapsed)
                .fraudScore(decision.getFraudScore())
                .authDecisionId(decision.getId())
                .build();
    }

    private FraudEngine.FraudEvaluationResult evaluateFraud(AuthorizationRequest request) {
        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(request.getCardId())
                .cardholderId(request.getCardholderId())
                .transactionId(request.getStan())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .merchantId(request.getMerchantId())
                .merchantCategory(request.getMerchantCategory())
                .countryCode(request.getCountryCode())
                .panHash(request.getPanHash())
                .timestamp(OffsetDateTime.now())
                .build();
        return fraudEngine.evaluateTransaction(ctx);
    }

    private boolean postApprovalActions(AuthorizationRequest request, AuthDecision decision) {
        if (request.getCardId() == null || request.getAmount() == null) return true;

        // Place hold via HoldService
        if (request.getCardholderId() != null) {
            try {
                List<CardAccount> cardAccounts = cardAccountService.getAccountsByCardholderId(request.getCardholderId());
                if (!cardAccounts.isEmpty()) {
                    CardAccount account = cardAccounts.get(0);
                    String transactionId = request.getStan() != null ? request.getStan() : UUID.randomUUID().toString();
                    HoldRecord hold = holdService.placeHold(
                            transactionId,
                            request.getCardId().toString(),
                            account.getId().toString(),
                            request.getAmount(),
                            request.getCurrencyCode(),
                            Duration.ofMinutes(30)
                    );
                    decision.setCardBalanceAfter(account.getAvailableBalance());
                    log.info("Hold placed: id={}, cardholderId={}, amount={}",
                            hold.getId(), request.getCardholderId(), request.getAmount());
                }
            } catch (Exception e) {
                log.warn("Failed to place hold for cardholderId={}: {}", request.getCardholderId(), e.getMessage());
                return false;
            }
        }

        // Update limit usage
        if (request.getAmount() != null) {
            List<CardLimitUsage> limits = cardLimits.computeIfAbsent(
                    request.getCardId(), k -> new ArrayList<>());
            for (CardLimitUsage limit : limits) {
                BigDecimal used = limit.getUsedAmount() != null ? limit.getUsedAmount() : BigDecimal.ZERO;
                limit.setUsedAmount(used.add(request.getAmount()));
                limit.setCountUsed((limit.getCountUsed() != null ? limit.getCountUsed() : 0) + 1);
                decision.setLimitUsed(limit.getUsedAmount());
                decision.setLimitMax(limit.getLimitAmount());
                decision.setVelocityUsed(limit.getCountUsed() != null ? limit.getCountUsed() : 0);
                decision.setVelocityMax(limit.getCountMax() != null ? limit.getCountMax() : 0);
            }
        }

        return true;
    }

    private boolean evaluateCondition(AuthRule rule, AuthorizationRequest request) {
        if (Boolean.TRUE.equals(rule.getApplyToAll())) return true;

        if (rule.getCardType() != null
                && !rule.getCardType().equalsIgnoreCase(request.getCardType())) {
            return false;
        }
        if (rule.getCardBrand() != null
                && !rule.getCardBrand().equalsIgnoreCase(request.getCardBrand())) {
            return false;
        }
        if (rule.getMerchantCategory() != null
                && !rule.getMerchantCategory().equals(request.getMerchantCategory())) {
            return false;
        }
        if (rule.getCountryCode() != null
                && !rule.getCountryCode().equals(request.getCountryCode())) {
            return false;
        }
        if (rule.getTimeStart() != null && rule.getTimeEnd() != null) {
            LocalTime now = LocalTime.now();
            if (now.isBefore(rule.getTimeStart()) || now.isAfter(rule.getTimeEnd())) {
                return false;
            }
        }
        if (rule.getDayOfWeek() != null
                && !rule.getDayOfWeek().equals(java.time.DayOfWeek.from(java.time.LocalDate.now()).name())) {
            return false;
        }
        return true;
    }

    private String checkCardLimits(AuthorizationRequest request) {
        List<CardLimitUsage> limits = cardLimits.get(request.getCardId());
        if (limits == null) return null;

        for (CardLimitUsage limit : limits) {
            if (limit.getLimitAmount() == null) continue;

            BigDecimal used = limit.getUsedAmount() != null
                    ? limit.getUsedAmount() : BigDecimal.ZERO;
            BigDecimal projected = used.add(request.getAmount() != null
                    ? request.getAmount() : BigDecimal.ZERO);

            if (projected.compareTo(limit.getLimitAmount()) > 0) {
                log.warn("Card limit exceeded: cardId={}, type={}, used={}, limit={}",
                        request.getCardId(), limit.getLimitType(), used, limit.getLimitAmount());
                return "Card limit exceeded: " + limit.getLimitType();
            }
        }
        return null;
    }

    private String checkVelocity(AuthorizationRequest request) {
        List<VelocityCheck> checks = velocityChecks.get(request.getCardId());
        if (checks == null) return null;

        for (VelocityCheck check : checks) {
            if (check.getMaxCount() != null
                    && check.getCurrentCount() != null
                    && check.getCurrentCount() >= check.getMaxCount()) {
                log.warn("Velocity exceeded: cardId={}, type={}, count={}, max={}",
                        request.getCardId(), check.getVelocityType(),
                        check.getCurrentCount(), check.getMaxCount());
                return "Velocity exceeded: " + check.getVelocityType();
            }
        }
        return null;
    }

    public AuthRule defineRule(AuthRule rule) {
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID());
        }
        if (rule.getStatus() == null) {
            rule.setStatus(RuleStatus.ACTIVE);
        }
        rule.setCreatedAt(OffsetDateTime.now());
        rule.setUpdatedAt(OffsetDateTime.now());
        rules.put(rule.getId(), rule);
        log.info("Auth rule defined: id={}, name={}, priority={}",
                rule.getId(), rule.getName(), rule.getPriority());
        return rule;
    }

    public AuthRule updateRule(UUID id, AuthRule rule) {
        AuthRule existing = rules.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        rule.setId(id);
        rule.setCreatedAt(existing.getCreatedAt());
        rule.setUpdatedAt(OffsetDateTime.now());
        rules.put(id, rule);
        log.info("Auth rule updated: id={}, name={}", id, rule.getName());
        return rule;
    }

    public List<AuthRule> getRules() {
        return List.copyOf(rules.values());
    }

    public List<AuthDecision> getDecisions(UUID cardId, int limit) {
        return decisions.stream()
                .filter(d -> cardId.equals(d.getCardId()))
                .sorted(Comparator.comparing(AuthDecision::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }

    public AuthDecision getDecisionByTransactionId(String txnId) {
        return decisions.stream()
                .filter(d -> txnId.equals(d.getTransactionId()))
                .findFirst()
                .orElse(null);
    }

    private AuthorizationResponse.Decision mapDecision(AuthDecision.Decision decision) {
        if (decision == null) return AuthorizationResponse.Decision.ERROR;
        return switch (decision) {
            case APPROVED -> AuthorizationResponse.Decision.APPROVED;
            case DECLINED -> AuthorizationResponse.Decision.DECLINED;
            case CHALLENGED -> AuthorizationResponse.Decision.CHALLENGED;
            case ERROR, TIMEOUT, REVIEW -> AuthorizationResponse.Decision.ERROR;
        };
    }

    @Data
    @Builder
    public static class AuthorizationRequest {
        private UUID cardId;
        private UUID cardholderId;
        private String merchantId;
        private String terminalId;
        private BigDecimal amount;
        private String currencyCode;
        private String mti;
        private String stan;
        private String panHash;
        private String cardType;
        private String cardBrand;
        private String merchantCategory;
        private String countryCode;
    }

    @Data
    @Builder
    public static class AuthorizationResponse {
        private Decision decision;
        private String responseCode;
        private String reason;
        private long processingTimeMs;
        private Integer fraudScore;
        private Long authDecisionId;

        public enum Decision {
            APPROVED, DECLINED, CHALLENGED, ERROR
        }
    }
}
