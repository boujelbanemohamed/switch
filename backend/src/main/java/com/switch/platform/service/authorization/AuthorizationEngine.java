package com.switch.platform.service.authorization;

import com.switch.platform.model.authorization.AuthDecision;
import com.switch.platform.model.authorization.AuthRule;
import com.switch.platform.model.authorization.AuthRule.ActionType;
import com.switch.platform.model.authorization.AuthRule.RuleStatus;
import com.switch.platform.model.authorization.CardLimitUsage;
import com.switch.platform.model.authorization.VelocityCheck;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        List<AuthRule> applicableRules = rules.values().stream()
                .filter(r -> r.getStatus() == RuleStatus.ACTIVE)
                .filter(r -> evaluateCondition(r, request))
                .sorted(Comparator.comparingInt(AuthRule::getPriority))
                .toList();

        AuthRule matchedRule = null;
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

        if (decision.getDecision() == AuthDecision.Decision.APPROVED
                || decision.getDecision() == null) {
            String limitReason = checkCardLimits(request);
            if (limitReason != null) {
                decision.setDecision(AuthDecision.Decision.DECLINED);
                decision.setResponseReason(limitReason);
                decision.setResponseCode("51");
            }
        }

        if (decision.getDecision() == AuthDecision.Decision.APPROVED) {
            String velocityReason = checkVelocity(request);
            if (velocityReason != null) {
                decision.setDecision(AuthDecision.Decision.DECLINED);
                decision.setResponseReason(velocityReason);
                decision.setResponseCode("61");
            }
        }

        if (decision.getDecision() == null) {
            decision.setDecision(AuthDecision.Decision.APPROVED);
            decision.setResponseCode("00");
            decision.setResponseReason("Approved");
        }

        if (matchedRule != null) {
            matchedRule.setSuccessCount(matchedRule.getSuccessCount() + 1);
        }

        long elapsed = System.currentTimeMillis() - start;
        decision.setProcessingTimeMs((int) elapsed);
        decision.setDecidedAt(OffsetDateTime.now());
        decisions.add(decision);

        log.info("Authorization result: {} reason={} time={}ms",
                decision.getDecision(), decision.getResponseReason(), elapsed);

        return AuthorizationResponse.builder()
                .decision(mapDecision(decision.getDecision()))
                .responseCode(decision.getResponseCode() != null
                        ? decision.getResponseCode() : "00")
                .reason(decision.getResponseReason())
                .processingTimeMs((int) elapsed)
                .authDecisionId(decision.getId())
                .build();
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
                && rule.getDayOfWeek() != java.time.DayOfWeek.from(java.time.LocalDate.now()).getValue()) {
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
