package com.switchplatform.platform.service.authorization;

import com.switchplatform.platform.model.authorization.AuthDecision;
import com.switchplatform.platform.model.authorization.AuthRule;
import com.switchplatform.platform.model.authorization.AuthRule.ActionType;
import com.switchplatform.platform.model.authorization.AuthRule.RuleStatus;
import com.switchplatform.platform.model.authorization.CardLimitUsage;
import com.switchplatform.platform.model.authorization.VelocityCheck;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.repository.authorization.CardLimitUsageRepository;
import com.switchplatform.platform.repository.authorization.AuthRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.repository.fraud.BehavioralProfileRepository;
import com.switchplatform.platform.repository.fraud.DeviceFingerprintRecordRepository;
import com.switchplatform.platform.repository.fraud.FraudAlertRepository;
import com.switchplatform.platform.repository.fraud.FraudRuleRepository;
import com.switchplatform.platform.service.fraud.BehavioralProfileService;
import com.switchplatform.platform.service.fraud.DeviceFingerprintService;
import com.switchplatform.platform.service.fraud.FraudEngine;
import com.switchplatform.platform.service.issuing.CardAccountService;
import com.switchplatform.platform.service.issuing.CardService;
import com.switchplatform.platform.repository.issuing.CardAccountRepository;
import com.switchplatform.platform.repository.authorization.HoldRecordRepository;
import com.switchplatform.platform.service.ledger.LedgerPostingEngine;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorizationEngineTest {

    private AuthorizationEngine authEngine;
    private CardLimitUsageRepository cardLimitUsageRepository;
    private AuthRuleRepository authRuleRepository;
    private CardService cardService;
    private CardAccountRepository cardAccountRepository;
    private CardAccount defaultAccount;
    private final Map<UUID, AuthRule> ruleStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        ruleStore.clear();
        cardLimitUsageRepository = mock(CardLimitUsageRepository.class);
        cardAccountRepository = mock(CardAccountRepository.class);
        cardService = mock(CardService.class);
        LedgerPostingEngine ledgerPostingEngine = mock(LedgerPostingEngine.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        authRuleRepository = mock(AuthRuleRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        UUID defaultCardAccountId = UUID.randomUUID();
        defaultAccount = CardAccount.builder()
                .id(defaultCardAccountId)
                .availableBalance(BigDecimal.valueOf(10000))
                .currencyCode("TND")
                .build();
        UUID defaultCardId = UUID.randomUUID();
        Card defaultCard = Card.builder()
                .id(defaultCardId)
                .cardAccountId(defaultCardAccountId)
                .build();
        when(cardService.getCard(any())).thenReturn(Optional.of(defaultCard));
        when(cardAccountRepository.findById(any())).thenReturn(Optional.of(defaultAccount));

        CardAccountService cardAccountService = new CardAccountService(cardAccountRepository);

        when(authRuleRepository.save(any())).thenAnswer(inv -> {
            AuthRule r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            ruleStore.put(r.getId(), r);
            return r;
        });
        when(authRuleRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(ruleStore.get(inv.getArgument(0))));
        when(authRuleRepository.findAll()).thenAnswer(inv -> new ArrayList<>(ruleStore.values()));
        when(authRuleRepository.findByStatusOrderByPriorityAsc(any())).thenAnswer(inv -> {
            AuthRule.RuleStatus status = inv.getArgument(0);
            return ruleStore.values().stream()
                    .filter(r -> r.getStatus() == status)
                    .sorted(Comparator.comparingInt(AuthRule::getPriority))
                    .toList();
        });
        when(authRuleRepository.existsById(any())).thenAnswer(inv ->
                ruleStore.containsKey(inv.getArgument(0)));
        doAnswer(inv -> {
            ruleStore.remove(inv.getArgument(0));
            return null;
        }).when(authRuleRepository).deleteById(any());

        authEngine = new AuthorizationEngine(
                new FraudEngine(new BehavioralProfileService(mock(BehavioralProfileRepository.class)),
                        mock(com.switchplatform.platform.repository.authorization.VelocityCheckRepository.class),
                        new DeviceFingerprintService(mock(DeviceFingerprintRecordRepository.class)),
                        mock(FraudRuleRepository.class), mock(FraudAlertRepository.class)),
                cardAccountService,
                cardService,
                new HoldService(mock(HoldRecordRepository.class), cardAccountService, ledgerPostingEngine, eventPublisher),
                cardLimitUsageRepository,
                ledgerPostingEngine,
                eventPublisher,
                authRuleRepository,
                objectMapper);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "'", e);
        }
    }

    @Test
    void shouldApproveTransaction() {
        AuthRule rule = AuthRule.builder()
                .name("Allow All")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response = authEngine.authorize(request);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.APPROVED, response.getDecision());
        assertEquals("00", response.getResponseCode());
    }

    @Test
    void shouldDeclineTransaction() {
        AuthRule rule = AuthRule.builder()
                .name("Decline All")
                .action(ActionType.DECLINE)
                .responseCode("05")
                .applyToAll(true)
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response = authEngine.authorize(request);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.DECLINED, response.getDecision());
    }

    @Test
    void shouldDeclineWhenLimitExceeded() {
        UUID cardId = UUID.randomUUID();

        CardLimitUsage limit = CardLimitUsage.builder()
                .cardId(cardId)
                .limitType(CardLimitUsage.LimitType.SINGLE)
                .limitAmount(BigDecimal.valueOf(100))
                .usedAmount(BigDecimal.ZERO)
                .build();

        when(cardLimitUsageRepository.findByCardId(cardId)).thenReturn(List.of(limit));

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .amount(BigDecimal.valueOf(200))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response = authEngine.authorize(request);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.DECLINED, response.getDecision());
        assertEquals("51", response.getResponseCode());
    }

    @Test
    void shouldDeclineWhenVelocityExceeded() {
        UUID cardId = UUID.randomUUID();

        AuthRule rule = AuthRule.builder()
                .name("Allow All")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        VelocityCheck check = VelocityCheck.builder()
                .cardId(cardId)
                .velocityType(VelocityCheck.VelocityType.TXNS_PER_DAY)
                .maxCount(1)
                .currentCount(0)
                .build();

        setField(authEngine, "velocityChecks",
                new ConcurrentHashMap<>(Map.of(cardId, new CopyOnWriteArrayList<>(List.of(check)))));

        AuthorizationEngine.AuthorizationRequest request1 = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .amount(BigDecimal.valueOf(50))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response1 = authEngine.authorize(request1);
        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.APPROVED, response1.getDecision());

        check.setCurrentCount(1);

        AuthorizationEngine.AuthorizationRequest request2 = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .amount(BigDecimal.valueOf(30))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response2 = authEngine.authorize(request2);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.DECLINED, response2.getDecision());
        assertEquals("61", response2.getResponseCode());
    }

    @Test
    void shouldApplyHighestPriorityRule() {
        AuthRule lowPriority = AuthRule.builder()
                .name("Low Priority Approve")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(100)
                .build();
        AuthRule highPriority = AuthRule.builder()
                .name("High Priority Decline")
                .action(ActionType.DECLINE)
                .responseCode("05")
                .applyToAll(true)
                .priority(1)
                .build();
        authEngine.defineRule(lowPriority);
        authEngine.defineRule(highPriority);

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .build();

        AuthorizationEngine.AuthorizationResponse response = authEngine.authorize(request);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.DECLINED, response.getDecision());
        assertEquals("05", response.getResponseCode());
    }

    @Test
    void shouldMatchByCardType() {
        AuthRule rule = AuthRule.builder()
                .name("Debit Only Rule")
                .action(ActionType.DECLINE)
                .responseCode("05")
                .cardType("DEBIT")
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .cardType("CREDIT")
                .build();

        AuthorizationEngine.AuthorizationResponse response = authEngine.authorize(request);

        assertEquals(AuthorizationEngine.AuthorizationResponse.Decision.APPROVED, response.getDecision());
        assertEquals("00", response.getResponseCode());
    }

    @Test
    void shouldLogDecision() {
        AuthRule rule = AuthRule.builder()
                .name("Allow All")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        UUID cardId = UUID.randomUUID();
        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .build();

        authEngine.authorize(request);

        List<AuthDecision> decisions = authEngine.getDecisions(cardId, 10);
        assertFalse(decisions.isEmpty());
        assertEquals(1, decisions.size());
    }

    @Test
    void shouldGetDecisionByTransactionId() {
        AuthRule rule = AuthRule.builder()
                .name("Allow All")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(100)
                .build();
        authEngine.defineRule(rule);

        UUID cardId = UUID.randomUUID();
        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .amount(BigDecimal.valueOf(100))
                .currencyCode("USD")
                .build();

        authEngine.authorize(request);

        String txnId = "TXN-001";
        List<AuthDecision> decisions = authEngine.getDecisions(cardId, 1);
        assertEquals(1, decisions.size());
        decisions.get(0).setTransactionId(txnId);

        AuthDecision found = authEngine.getDecisionByTransactionId(txnId);
        assertNotNull(found);
        assertEquals(txnId, found.getTransactionId());
    }

    @Test
    void shouldDefineAndUpdateRule() {
        AuthRule rule = AuthRule.builder()
                .name("Original Rule")
                .action(ActionType.APPROVE)
                .responseCode("00")
                .applyToAll(true)
                .priority(50)
                .build();

        AuthRule defined = authEngine.defineRule(rule);
        assertNotNull(defined.getId());
        assertEquals("Original Rule", defined.getName());
        assertEquals(ActionType.APPROVE, defined.getAction());
        assertEquals(RuleStatus.ACTIVE, defined.getStatus());
        assertNotNull(defined.getCreatedAt());
        assertNotNull(defined.getUpdatedAt());

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Updated Rule");
        updates.put("action", ActionType.DECLINE);
        updates.put("responseCode", "05");
        updates.put("priority", 10);

        AuthRule updated = authEngine.updateRule(defined.getId(), updates);
        assertEquals(defined.getId(), updated.getId());
        assertEquals("Updated Rule", updated.getName());
        assertEquals(ActionType.DECLINE, updated.getAction());
        assertEquals("05", updated.getResponseCode());
        assertEquals(10, updated.getPriority());
        assertEquals(defined.getCreatedAt(), updated.getCreatedAt());
        assertNotNull(updated.getUpdatedAt());

        List<AuthRule> allRules = authEngine.getRules();
        assertEquals(1, allRules.size());
        assertEquals("Updated Rule", allRules.get(0).getName());
    }
}
