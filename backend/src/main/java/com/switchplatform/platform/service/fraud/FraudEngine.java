package com.switchplatform.platform.service.fraud;

import com.switchplatform.platform.model.fraud.FraudAlert;
import com.switchplatform.platform.model.fraud.FraudRule;
import com.switchplatform.platform.model.authorization.VelocityCheck;
import com.switchplatform.platform.repository.authorization.VelocityCheckRepository;
import com.switchplatform.platform.repository.fraud.FraudAlertRepository;
import com.switchplatform.platform.repository.fraud.FraudRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudEngine {

    private final Map<UUID, FraudRule> rules = new ConcurrentHashMap<>();
    private final List<FraudAlert> alerts = new CopyOnWriteArrayList<>();
    private final BehavioralProfileService profileService;
    private final VelocityCheckRepository velocityCheckRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;

    private static final long VELOCITY_WINDOW_MINUTES = 60;

    @PostConstruct
    public void loadRules() {
        try {
            List<FraudRule> dbRules = fraudRuleRepository.findAll();
            if (!dbRules.isEmpty()) {
                for (FraudRule rule : dbRules) {
                    rules.put(rule.getId(), rule);
                }
                log.info("Loaded {} fraud rules from database", dbRules.size());
                return;
            }
        } catch (Exception e) {
            log.warn("Could not load fraud rules from database, using defaults: {}", e.getMessage());
        }
        initDefaultRules();
        initHighRiskPatterns();
    }

    @Transactional
    protected void initDefaultRules() {
        if (!rules.isEmpty()) return;

        FraudRule highAmountRule = FraudRule.builder()
                .id(UUID.randomUUID())
                .name("High Amount Default")
                .description("Decline transactions over 10000")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.HIGH)
                .action(FraudRule.Action.FLAG)
                .conditionExpression("amount > 10000")
                .scoreWeight(30)
                .status(FraudRule.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        rules.put(highAmountRule.getId(), highAmountRule);

        FraudRule foreignTxnRule = FraudRule.builder()
                .id(UUID.randomUUID())
                .name("Foreign Transaction Default")
                .description("Flag foreign transactions")
                .ruleCategory(FraudRule.RuleCategory.GEO)
                .severity(FraudRule.Severity.MEDIUM)
                .action(FraudRule.Action.FLAG)
                .conditionExpression("is_foreign")
                .scoreWeight(20)
                .status(FraudRule.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        rules.put(foreignTxnRule.getId(), foreignTxnRule);

        log.info("Initialized {} default fraud rules", rules.size());
    }

    protected void initHighRiskPatterns() {
    }

    public FraudEvaluationResult evaluateTransaction(EvaluationContext ctx) {
        log.info("Fraud evaluation: cardId={}, txnId={}, amount={}",
                ctx.getCardId(), ctx.getTransactionId(), ctx.getAmount());

        List<FraudRule> activeRules = rules.values().stream()
                .filter(r -> r.getStatus() == FraudRule.Status.ACTIVE)
                .toList();

        int totalScore = 0;
        List<String> matchedRuleNames = new ArrayList<>();
        List<UUID> alertIds = new ArrayList<>();
        int dynamicScore = 0;

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
                if (alert.getId() == null) {
                    alert.setId(UUID.randomUUID());
                }
                alerts.add(alert);
                alertIds.add(alert.getId());

                log.debug("Fraud rule matched: {}, weight={}, total={}",
                        rule.getName(), weight, totalScore);
            }
        }

        if (ctx.getCardholderId() != null) {
            BehavioralProfileService.AnomalyResult anomaly = profileService.detectAnomalies(
                    ctx.getCardholderId(), ctx.getAmount(), ctx.getMerchantCategory(),
                    ctx.getCountryCode(),
                    ctx.getTimestamp() != null ? ctx.getTimestamp().toLocalDateTime() : LocalDateTime.now());
            dynamicScore += anomaly.getScore();
            if (!anomaly.getReasons().isEmpty()) {
                log.info("Behavioral anomalies detected: {} (score={})",
                        anomaly.getReasons(), anomaly.getScore());
            }
        }

        int velocityScore = checkVelocity(ctx);
        dynamicScore += velocityScore;

        String deviceRecommendation = null;
        if (ctx.getDeviceId() != null && ctx.getCardId() != null) {
            try {
                DeviceScoreResult deviceResult = deviceFingerprintService.evaluate(
                        ctx.getCardId().toString(),
                        ctx.getDeviceId(),
                        ctx.getDeviceType(),
                        ctx.getOs(),
                        ctx.getBrowser(),
                        ctx.getUserAgent(),
                        ctx.getIpAddress()
                );
                int deviceScore = (int) Math.round(deviceResult.getScore() * 100);
                dynamicScore += deviceScore;
                deviceRecommendation = deviceResult.getRecommendation();
                log.info("Device fingerprint score={}, recommendation={}",
                        deviceScore, deviceRecommendation);
            } catch (Exception e) {
                log.warn("Device fingerprint evaluation failed", e);
            }
        }

        totalScore += dynamicScore;
        totalScore = Math.min(totalScore, 100);

        RiskLevel riskLevel;
        FraudAction action;

        if ("BLOCK".equals(deviceRecommendation)) {
            riskLevel = RiskLevel.CRITICAL;
            action = FraudAction.BLOCK;
        } else if (totalScore > 80) {
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

        recordVelocity(ctx);

        log.info("Fraud result: score={} (dynamic={}), risk={}, action={}, rules={}",
                totalScore, dynamicScore, riskLevel, action, matchedRuleNames.size());

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
        Objects.requireNonNull(category, "Rule category must not be null for rule: " + rule.getName());

        return switch (category) {
            case AMOUNT -> ctx.getAmount() != null
                    && ctx.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0;
            case GEO -> ctx.getCountryCode() != null
                    && !ctx.getCountryCode().equalsIgnoreCase(ctx.getPreviousCountryCode());
            case VELOCITY -> checkVelocity(ctx) > 0;
            case BEHAVIORAL -> ctx.getCardholderId() != null
                    && profileService.detectAnomalies(ctx.getCardholderId(), ctx.getAmount(),
                            ctx.getMerchantCategory(), ctx.getCountryCode(),
                            ctx.getTimestamp() != null ? ctx.getTimestamp().toLocalDateTime()
                                    : LocalDateTime.now()).getScore() > 0;
            case DEVICE -> ctx.getDeviceId() != null && ctx.getPreviousDeviceId() != null
                    && !ctx.getDeviceId().equals(ctx.getPreviousDeviceId());
            case MERCHANT -> ctx.getMerchantId() != null && ctx.getPreviousMerchantId() != null
                    && !ctx.getMerchantId().equals(ctx.getPreviousMerchantId());
            default -> true;
        };
    }

    private boolean evaluateExpression(String expr, EvaluationContext ctx) {
        try {
            String e = expr.trim();

            if (e.startsWith("amount > ")) {
                BigDecimal threshold = new BigDecimal(e.substring(9).trim());
                return ctx.getAmount() != null && ctx.getAmount().compareTo(threshold) > 0;
            }
            if (e.startsWith("amount < ")) {
                BigDecimal threshold = new BigDecimal(e.substring(9).trim());
                return ctx.getAmount() != null && ctx.getAmount().compareTo(threshold) < 0;
            }
            if (e.startsWith("country != ")) {
                String country = e.substring(11).trim().replaceAll("['\"]", "");
                return ctx.getCountryCode() != null && !ctx.getCountryCode().equalsIgnoreCase(country);
            }
            if (e.startsWith("country == ")) {
                String country = e.substring(11).trim().replaceAll("['\"]", "");
                return ctx.getCountryCode() != null && ctx.getCountryCode().equalsIgnoreCase(country);
            }
            if (e.startsWith("mcc == ")) {
                String mcc = e.substring(7).trim().replaceAll("['\"]", "");
                return ctx.getMerchantCategory() != null && ctx.getMerchantCategory().equals(mcc);
            }
            if (e.startsWith("mcc != ")) {
                String mcc = e.substring(7).trim().replaceAll("['\"]", "");
                return ctx.getMerchantCategory() != null && !ctx.getMerchantCategory().equals(mcc);
            }
            if (e.equals("is_foreign")) {
                return ctx.getCountryCode() != null && !"TN".equalsIgnoreCase(ctx.getCountryCode());
            }
            if (e.equals("is_high_amount")) {
                return ctx.getAmount() != null && ctx.getAmount().compareTo(BigDecimal.valueOf(5000)) > 0;
            }

            log.warn("Unsupported expression: {}", expr);
            return false;
        } catch (Exception ex) {
            log.error("Failed to evaluate expression: {}", expr, ex);
            return false;
        }
    }

    private int checkVelocity(EvaluationContext ctx) {
        if (ctx.getCardId() == null) return 0;

        int count = countRecentTransactions(ctx.getCardId(), VELOCITY_WINDOW_MINUTES);
        if (count == 0) return 0;

        int score = 0;

        if (count > 10) {
            score += 30;
        } else if (count > 5) {
            score += 15;
        } else if (count > 3) {
            score += 5;
        }

        List<VelocityCheck> recent = velocityCheckRepository.findByCardIdAndCreatedAtAfter(
                ctx.getCardId(), OffsetDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES));

        BigDecimal totalAmount = recent.stream()
                .map(VelocityCheck::getCurrentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            score += 20;
        } else if (totalAmount.compareTo(BigDecimal.valueOf(20000)) > 0) {
            score += 10;
        }

        return score;
    }

    private void recordVelocity(EvaluationContext ctx) {
        if (ctx.getCardId() == null) return;
        recordForVelocity(ctx.getCardId(), ctx.getAmount());
    }

    private void recordForVelocity(UUID cardId, BigDecimal amount) {
        VelocityCheck vc = VelocityCheck.builder()
                .cardId(cardId)
                .currentAmount(amount)
                .velocityType(VelocityCheck.VelocityType.TXNS_PER_HOUR)
                .createdAt(OffsetDateTime.now())
                .build();
        velocityCheckRepository.save(vc);
    }

    private int countRecentTransactions(UUID cardId, long windowMinutes) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(windowMinutes);
        return (int) velocityCheckRepository.countByCardIdAndCreatedAtAfter(cardId, since);
    }

    public List<FraudRule> listRules() {
        return rules.values().stream()
                .sorted(Comparator.comparing(FraudRule::getCreatedAt).reversed())
                .toList();
    }

    public FraudRule defineRule(FraudRule rule) {
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID());
        }
        if (rule.getStatus() == null) {
            rule.setStatus(FraudRule.Status.ACTIVE);
        }
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(OffsetDateTime.now());
        }
        rule.setUpdatedAt(OffsetDateTime.now());
        rules.put(rule.getId(), rule);
        log.info("Fraud rule defined: id={}, name={}, category={}",
                rule.getId(), rule.getName(), rule.getRuleCategory());
        return rule;
    }

    public void deleteRule(UUID ruleId) {
        rules.remove(ruleId);
        log.info("Fraud rule deleted: id={}", ruleId);
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

    public List<FraudAlert> getAllAlerts() {
        return alerts.stream()
                .sorted(Comparator.comparing(FraudAlert::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, Long> getAlertStats() {
        return alerts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus().name(), Collectors.counting()));
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
        private String previousMerchantId;
        private String merchantCategory;
        private String countryCode;
        private String previousCountryCode;
        private String deviceId;
        private String previousDeviceId;
        private String deviceType;
        private String os;
        private String browser;
        private String userAgent;
        private String ipAddress;
        private OffsetDateTime timestamp;
        private String panHash;
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

    private record VelocityRecord(BigDecimal amount, String countryCode, OffsetDateTime timestamp) {}
}
