package com.switchplatform.platform.service.fraud;

import com.switchplatform.platform.model.fraud.FraudAlert;
import com.switchplatform.platform.model.fraud.FraudRule;
import com.switchplatform.platform.repository.authorization.VelocityCheckRepository;
import com.switchplatform.platform.repository.fraud.BehavioralProfileRepository;
import com.switchplatform.platform.repository.fraud.DeviceFingerprintRecordRepository;
import com.switchplatform.platform.repository.fraud.FraudAlertRepository;
import com.switchplatform.platform.repository.fraud.FraudRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FraudEngineTest {

    private FraudEngine fraudEngine;
    private BehavioralProfileService profileService;
    private DeviceFingerprintService deviceFingerprintService;

    @BeforeEach
    void setUp() {
        profileService = new BehavioralProfileService(mock(BehavioralProfileRepository.class));
        deviceFingerprintService = new DeviceFingerprintService(mock(DeviceFingerprintRecordRepository.class));
        fraudEngine = new FraudEngine(profileService, mock(VelocityCheckRepository.class),
                deviceFingerprintService,
                mock(FraudRuleRepository.class), mock(FraudAlertRepository.class));
    }

    @Test
    void shouldAllowTransactionWithLowScore() {
        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .transactionId("TXN-001")
                .amount(BigDecimal.valueOf(50))
                .currencyCode("USD")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);

        assertEquals(0, result.getScore());
        assertEquals(FraudEngine.RiskLevel.LOW, result.getRiskLevel());
        assertEquals(FraudEngine.FraudAction.ALLOW, result.getAction());
        assertTrue(result.getMatchedRules().isEmpty());
        assertTrue(result.getAlerts().isEmpty());
    }

    @Test
    void shouldFlagTransactionWithMediumScore() {
        FraudRule rule = FraudRule.builder()
                .name("Amount Check")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.HIGH)
                .scoreWeight(60)
                .action(FraudRule.Action.FLAG)
                .build();
        fraudEngine.defineRule(rule);

        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .transactionId("TXN-002")
                .amount(BigDecimal.valueOf(15000))
                .currencyCode("USD")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);

        assertTrue(result.getScore() > 50);
        assertEquals(FraudEngine.RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(FraudEngine.FraudAction.FLAG, result.getAction());
        assertEquals(1, result.getMatchedRules().size());
        assertEquals("Amount Check", result.getMatchedRules().get(0));
    }

    @Test
    void shouldBlockTransactionWithHighScore() {
        FraudRule rule1 = FraudRule.builder()
                .name("High Amount")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.CRITICAL)
                .scoreWeight(50)
                .action(FraudRule.Action.BLOCK)
                .build();
        FraudRule rule2 = FraudRule.builder()
                .name("Suspicious Geo")
                .ruleCategory(FraudRule.RuleCategory.GEO)
                .severity(FraudRule.Severity.HIGH)
                .scoreWeight(40)
                .action(FraudRule.Action.BLOCK)
                .build();
        fraudEngine.defineRule(rule1);
        fraudEngine.defineRule(rule2);

        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .transactionId("TXN-003")
                .amount(BigDecimal.valueOf(20000))
                .currencyCode("USD")
                .countryCode("XX")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);

        assertTrue(result.getScore() > 80);
        assertEquals(FraudEngine.RiskLevel.CRITICAL, result.getRiskLevel());
        assertEquals(FraudEngine.FraudAction.BLOCK, result.getAction());
        assertEquals(2, result.getMatchedRules().size());
    }

    @Test
    void shouldCreateAlertForHighScoreTransaction() {
        FraudRule rule = FraudRule.builder()
                .name("Critical Amount")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.CRITICAL)
                .scoreWeight(90)
                .build();
        fraudEngine.defineRule(rule);

        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .cardholderId(UUID.randomUUID())
                .transactionId("TXN-004")
                .amount(BigDecimal.valueOf(50000))
                .currencyCode("USD")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);

        assertEquals(1, result.getAlerts().size());
        assertFalse(result.getMatchedRules().isEmpty());

        UUID alertId = result.getAlerts().get(0);
        List<FraudAlert> alerts = fraudEngine.getAlertsByCard(ctx.getCardId());
        assertEquals(1, alerts.size());
        FraudAlert alert = alerts.get(0);
        assertEquals(alertId, alert.getId());
        assertEquals(FraudAlert.Status.OPEN, alert.getStatus());
        assertEquals(ctx.getTransactionId(), alert.getTransactionId());
        assertEquals(ctx.getCardId(), alert.getCardId());
        assertEquals(rule.getId(), alert.getRuleId());
    }

    @Test
    void shouldConfirmFraud() {
        FraudRule rule = FraudRule.builder()
                .name("Confirm Test Rule")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.CRITICAL)
                .scoreWeight(90)
                .build();
        fraudEngine.defineRule(rule);

        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .cardholderId(UUID.randomUUID())
                .transactionId("TXN-005")
                .amount(BigDecimal.valueOf(50000))
                .currencyCode("USD")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);
        UUID alertId = result.getAlerts().get(0);

        FraudAlert confirmed = fraudEngine.confirmFraud(alertId);

        assertEquals(FraudAlert.Status.CONFIRMED, confirmed.getStatus());
        assertEquals(FraudAlert.Decision.DECLINED, confirmed.getDecision());
        assertNotNull(confirmed.getResolvedAt());

        List<FraudAlert> alerts = fraudEngine.getAlertsByStatus("CONFIRMED");
        assertTrue(alerts.stream().anyMatch(a -> a.getId().equals(alertId)));
    }

    @Test
    void shouldDismissAsFalsePositive() {
        FraudRule rule = FraudRule.builder()
                .name("False Positive Test Rule")
                .ruleCategory(FraudRule.RuleCategory.AMOUNT)
                .severity(FraudRule.Severity.HIGH)
                .scoreWeight(60)
                .build();
        fraudEngine.defineRule(rule);

        FraudEngine.EvaluationContext ctx = FraudEngine.EvaluationContext.builder()
                .cardId(UUID.randomUUID())
                .cardholderId(UUID.randomUUID())
                .transactionId("TXN-006")
                .amount(BigDecimal.valueOf(15000))
                .currencyCode("USD")
                .build();

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctx);
        UUID alertId = result.getAlerts().get(0);

        FraudAlert dismissed = fraudEngine.dismissAsFalsePositive(alertId);

        assertEquals(FraudAlert.Status.FALSE_POSITIVE, dismissed.getStatus());
        assertEquals(FraudAlert.Decision.APPROVED, dismissed.getDecision());
        assertNotNull(dismissed.getResolvedAt());

        List<FraudAlert> alerts = fraudEngine.getAlertsByStatus("FALSE_POSITIVE");
        assertTrue(alerts.stream().anyMatch(a -> a.getId().equals(alertId)));
    }
}
