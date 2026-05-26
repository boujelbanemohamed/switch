package com.switch.platform.service.fraud;

import com.switch.platform.model.fraud.FraudAlert;
import com.switch.platform.model.fraud.FraudRule;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudEngine {

    private final Map<UUID, FraudRule> rules = new ConcurrentHashMap<>();
    private final List<FraudAlert> alerts = new CopyOnWriteArrayList<>();

    public FraudEvaluationResult evaluateTransaction(EvaluationContext ctx) {
        log.info("Fraud evaluation: cardId={}, txnId={}, amount={}",
                ctx.getCardId(), ctx.getTransactionId(), ctx.getAmount());

        List<FraudRule> activeRules = rules.values().stream()
                .filter(r -> r.getStatus() == FraudRule.Status.ACTIVE)
                .toList();

        int totalScore = 0;
        List<String> matchedRuleNames = new ArrayList<>();
        List<UUID> alertIds = new ArrayList<>();

        for (FraudRule rule : activeRules) {
            if (matchesCondition(rule, ctx)) {
                matchedRuleNames.add(rule.getName());
                int weight = rule.getScoreWeight() != null ? rule.getScoreWeight() : 0;
                totalScore += weight;

                FraudAlert alert = FraudAlert.builder()
                        .cardId(ctx.getCardId())
                        .cardholderId(ctx.getCardholderId())
                        .transactionId(ctx.getTransactionId())
                        .ruleId(rule.getId())
                        .alertType(rule.getRuleCategory().name())
                        .severity(rule.getSeverity().name())
                        .score(weight)
                        .description("Matched rule: " + rule.getName())
                        .status(FraudAlert.Status.OPEN)
                        .decision(FraudAlert.Decision.REVIEW)
                        .build();
                alerts.add(alert);
                alertIds.add(alert.getId());

                log.debug("Fraud rule matched: {}, weight={}, total={}",
                        rule.getName(), weight, totalScore);
            }
        }

        totalScore = Math.min(totalScore, 100);

        RiskLevel riskLevel;
        FraudAction action;
        if (totalScore > 80) {
            riskLevel = RiskLevel.CRITICAL;
            action = FraudAction.BLOCK;
        } else if (totalScore > 50) {
            riskLevel = RiskLevel.HIGH;
            action = FraudAction.FLAG;
        } else if (totalScore > 20) {
            riskLevel = RiskLevel.MEDIUM;
            action = FraudAction.ALLOW;
        } else {
            riskLevel = RiskLevel.LOW;
            action = FraudAction.ALLOW;
        }

        log.info("Fraud result: score={}, risk={}, action={}, rules={}",
                totalScore, riskLevel, action, matchedRuleNames.size());

        return FraudEvaluationResult.builder()
                .score(totalScore)
                .riskLevel(riskLevel)
                .action(action)
                .matchedRules(matchedRuleNames)
                .alerts(alertIds)
                .build();
    }

    private boolean matchesCondition(FraudRule rule, EvaluationContext ctx) {
        String expr = rule.getConditionExpression();
        if (expr != null && !expr.isBlank()) {
            return evaluateExpression(expr, ctx);
        }
        FraudRule.RuleCategory category = rule.getRuleCategory();
        if (category == FraudRule.RuleCategory.AMOUNT && ctx.getAmount() != null) {
            return ctx.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0;
        }
        if (category == FraudRule.RuleCategory.GEO && ctx.getCountryCode() != null) {
            return true;
        }
        if (category == FraudRule.RuleCategory.VELOCITY) {
            return true;
        }
        return true;
    }

    private boolean evaluateExpression(String expr, EvaluationContext ctx) {
        return true;
    }

    public FraudRule defineRule(FraudRule rule) {
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID());
        }
        rule.setCreatedAt(OffsetDateTime.now());
        rule.setUpdatedAt(OffsetDateTime.now());
        rules.put(rule.getId(), rule);
        log.info("Fraud rule defined: id={}, name={}, category={}",
                rule.getId(), rule.getName(), rule.getRuleCategory());
        return rule;
    }

    public List<FraudAlert> getAlertsByStatus(String status) {
        return alerts.stream()
                .filter(a -> a.getStatus() != null
                        && a.getStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(FraudAlert::getCreatedAt).reversed())
                .toList();
    }

    public List<FraudAlert> getAlertsByCard(UUID cardId) {
        return alerts.stream()
                .filter(a -> cardId.equals(a.getCardId()))
                .sorted(Comparator.comparing(FraudAlert::getCreatedAt).reversed())
                .toList();
    }

    public FraudAlert updateAlertStatus(UUID alertId, String status, String decision) {
        FraudAlert alert = alerts.stream()
                .filter(a -> alertId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.setStatus(FraudAlert.Status.valueOf(status.toUpperCase()));
        alert.setDecision(FraudAlert.Decision.valueOf(decision.toUpperCase()));
        alert.setResolvedAt(OffsetDateTime.now());
        log.info("Alert updated: id={}, status={}, decision={}", alertId, status, decision);
        return alert;
    }

    public FraudAlert confirmFraud(UUID alertId) {
        FraudAlert alert = updateAlertStatus(alertId, "CONFIRMED", "DECLINED");
        if (alert.getRuleId() != null) {
            FraudRule rule = rules.get(alert.getRuleId());
            if (rule != null) {
                rule.setTruePositiveCount(
                        (rule.getTruePositiveCount() != null ? rule.getTruePositiveCount() : 0) + 1);
            }
        }
        return alert;
    }

    public FraudAlert dismissAsFalsePositive(UUID alertId) {
        FraudAlert alert = updateAlertStatus(alertId, "FALSE_POSITIVE", "APPROVED");
        if (alert.getRuleId() != null) {
            FraudRule rule = rules.get(alert.getRuleId());
            if (rule != null) {
                rule.setFalsePositiveCount(
                        (rule.getFalsePositiveCount() != null ? rule.getFalsePositiveCount() : 0) + 1);
            }
        }
        return alert;
    }

    @Data
    @Builder
    public static class EvaluationContext {
        private UUID cardId;
        private UUID cardholderId;
        private String transactionId;
        private BigDecimal amount;
        private String currencyCode;
        private String merchantId;
        private String merchantCategory;
        private String countryCode;
        private OffsetDateTime timestamp;
        private String panHash;
        private String deviceId;
    }

    @Data
    @Builder
    public static class FraudEvaluationResult {
        private int score;
        private RiskLevel riskLevel;
        private FraudAction action;
        private List<String> matchedRules;
        private List<UUID> alerts;
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum FraudAction {
        ALLOW, FLAG, BLOCK
    }
}
