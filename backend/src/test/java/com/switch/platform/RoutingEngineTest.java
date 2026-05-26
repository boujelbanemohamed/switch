package com.switch.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switch.platform.model.Participant;
import com.switch.platform.model.RoutingRule;
import com.switch.platform.repository.RoutingRuleRepository;
import com.switch.platform.router.RoutingContext;
import com.switch.platform.router.RoutingEngine;
import com.switch.platform.router.RoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingEngineTest {

    @Mock
    private RoutingRuleRepository routingRuleRepository;

    private RoutingEngine routingEngine;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        routingEngine = new RoutingEngine(routingRuleRepository, objectMapper);
    }

    @Test
    void shouldRouteByCurrency() {
        Participant dest = Participant.builder()
                .id(UUID.randomUUID())
                .code("BANK_B")
                .name("Bank B")
                .build();

        RoutingRule rule = RoutingRule.builder()
                .id(UUID.randomUUID())
                .name("TND Routing")
                .priority(100)
                .destinationParticipant(dest)
                .conditionExpression("{\"operator\":\"AND\",\"rules\":[{\"field\":\"CURRENCY\",\"operator\":\"EQUALS\",\"value\":\"TND\"}]}")
                .status(RoutingRule.RuleStatus.ACTIVE)
                .build();

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.BOTH))
                .thenReturn(List.of(rule));

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.ISO8583))
                .thenReturn(List.of());

        RoutingContext context = RoutingContext.builder()
                .pan("5500001234567890")
                .bin("550000")
                .amount(BigDecimal.valueOf(100))
                .currencyCode("TND")
                .mti("0200")
                .protocol("ISO8583")
                .build();

        RoutingResult result = routingEngine.route(context);

        assertTrue(result.isMatched());
        assertEquals("BANK_B", result.getDestinationParticipant().getCode());
    }

    @Test
    void shouldNotRouteWhenNoMatch() {
        Participant dest = Participant.builder()
                .id(UUID.randomUUID())
                .code("BANK_B")
                .name("Bank B")
                .build();

        RoutingRule rule = RoutingRule.builder()
                .id(UUID.randomUUID())
                .name("EUR Routing")
                .priority(100)
                .destinationParticipant(dest)
                .conditionExpression("{\"operator\":\"AND\",\"rules\":[{\"field\":\"CURRENCY\",\"operator\":\"EQUALS\",\"value\":\"EUR\"}]}")
                .status(RoutingRule.RuleStatus.ACTIVE)
                .build();

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.BOTH))
                .thenReturn(List.of(rule));

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.ISO8583))
                .thenReturn(List.of());

        RoutingContext context = RoutingContext.builder()
                .pan("4000001234567890")
                .bin("400000")
                .currencyCode("TND")
                .mti("0200")
                .protocol("ISO8583")
                .build();

        RoutingResult result = routingEngine.route(context);

        assertFalse(result.isMatched());
    }

    @Test
    void shouldRouteByBinRange() {
        Participant dest = Participant.builder()
                .id(UUID.randomUUID())
                .code("BANK_C")
                .build();

        RoutingRule rule = RoutingRule.builder()
                .id(UUID.randomUUID())
                .name("BIN Range Rule")
                .priority(100)
                .destinationParticipant(dest)
                .conditionExpression("{\"operator\":\"AND\",\"rules\":[{\"field\":\"BIN\",\"operator\":\"BIN_RANGE\",\"min\":\"400000\",\"max\":\"499999\"}]}")
                .status(RoutingRule.RuleStatus.ACTIVE)
                .build();

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.BOTH))
                .thenReturn(List.of(rule));

        when(routingRuleRepository.findByStatusAndProtocolOrderByPriorityAsc(
                RoutingRule.RuleStatus.ACTIVE, RoutingRule.Protocol.ISO8583))
                .thenReturn(List.of());

        RoutingContext context = RoutingContext.builder()
                .pan("4500001234567890")
                .bin("450000")
                .protocol("ISO8583")
                .build();

        RoutingResult result = routingEngine.route(context);
        assertTrue(result.isMatched());
    }
}
