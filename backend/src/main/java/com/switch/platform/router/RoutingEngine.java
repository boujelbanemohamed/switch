package com.switch.platform.router;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switch.platform.model.Participant;
import com.switch.platform.model.RoutingRule;
import com.switch.platform.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingEngine {

    private final RoutingRuleRepository routingRuleRepository;
    private final ObjectMapper objectMapper;

    public RoutingResult route(RoutingContext context) {
        List<RoutingRule> rules = routingRuleRepository
                .findByStatusAndProtocolOrderByPriorityAsc(
                        RoutingRule.RuleStatus.ACTIVE,
                        RoutingRule.Protocol.BOTH);

        List<RoutingRule> allRules = new ArrayList<>(rules);

        RoutingRule.Protocol targetProtocol = RoutingRule.Protocol.valueOf(
                context.getProtocol());

        List<RoutingRule> protocolRules = routingRuleRepository
                .findByStatusAndProtocolOrderByPriorityAsc(
                        RoutingRule.RuleStatus.ACTIVE, targetProtocol);
        allRules.addAll(protocolRules);

        allRules = allRules.stream()
                .distinct()
                .sorted(Comparator.comparingInt(RoutingRule::getPriority))
                .collect(Collectors.toList());

        for (RoutingRule rule : allRules) {
            if (evaluateConditions(rule, context)) {
                log.info("Routing match: rule={} (priority={}) -> destination={}",
                        rule.getName(), rule.getPriority(),
                        rule.getDestinationParticipant().getCode());

                return RoutingResult.builder()
                        .matched(true)
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .sourceParticipant(rule.getSourceParticipant())
                        .destinationParticipant(rule.getDestinationParticipant())
                        .build();
            }
        }

        log.warn("No routing rule matched for context: {}", context);
        return RoutingResult.builder()
                .matched(false)
                .build();
    }

    private boolean evaluateConditions(RoutingRule rule, RoutingContext context) {
        try {
            if (rule.getConditionExpression() == null || rule.getConditionExpression().isBlank()) {
                return true;
            }

            Map<String, Object> conditions = objectMapper.readValue(
                    rule.getConditionExpression(),
                    new TypeReference<Map<String, Object>>() {});

            return evaluateConditionGroup(conditions, context);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse condition expression for rule {}: {}",
                    rule.getId(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateConditionGroup(Map<String, Object> conditions, RoutingContext context) {
        String operator = (String) conditions.getOrDefault("operator", "AND");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) conditions.get("rules");

        if (rules == null || rules.isEmpty()) {
            return true;
        }

        boolean result = operator.equalsIgnoreCase("AND");

        for (Map<String, Object> rule : rules) {
            boolean ruleResult = evaluateSingleCondition(rule, context);

            if (operator.equalsIgnoreCase("AND")) {
                result = result && ruleResult;
                if (!result) break;
            } else {
                result = result || ruleResult;
                if (result) break;
            }
        }

        return result;
    }

    private boolean evaluateSingleCondition(Map<String, Object> condition, RoutingContext context) {
        String field = (String) condition.get("field");
        String op = (String) condition.get("operator");
        Object value = condition.get("value");

        if (field == null || op == null) return true;

        String contextValue = getContextValue(context, field);
        if (contextValue == null) return false;

        return switch (op.toUpperCase()) {
            case "EQUALS" -> contextValue.equalsIgnoreCase(String.valueOf(value));
            case "NOT_EQUALS" -> !contextValue.equalsIgnoreCase(String.valueOf(value));
            case "STARTS_WITH" -> contextValue.startsWith(String.valueOf(value));
            case "CONTAINS" -> contextValue.contains(String.valueOf(value));
            case "IN" -> {
                List<String> values = (List<String>) condition.get("values");
                yield values != null && values.stream()
                        .anyMatch(v -> contextValue.equalsIgnoreCase(v));
            }
            case "BIN_RANGE" -> evaluateBinRange(contextValue, condition);
            case "AMOUNT_RANGE" -> evaluateAmountRange(context, condition);
            default -> {
                log.warn("Unknown routing operator: {}", op);
                yield false;
            }
        };
    }

    private boolean evaluateBinRange(String bin, Map<String, Object> condition) {
        Long binValue = safeParseLong(bin);
        Long minBin = safeParseLong(String.valueOf(condition.get("min")));
        Long maxBin = safeParseLong(String.valueOf(condition.get("max")));

        if (binValue == null || minBin == null || maxBin == null) return false;
        return binValue >= minBin && binValue <= maxBin;
    }

    private boolean evaluateAmountRange(RoutingContext context, Map<String, Object> condition) {
        Long amount = safeParseLong(context.getAmount());
        Long minAmt = safeParseLong(String.valueOf(condition.get("min")));
        Long maxAmt = safeParseLong(String.valueOf(condition.get("max")));

        if (amount == null) return false;
        if (minAmt != null && amount < minAmt) return false;
        if (maxAmt != null && amount > maxAmt) return false;
        return true;
    }

    private String getContextValue(RoutingContext context, String field) {
        return switch (field.toUpperCase()) {
            case "PAN", "BIN" -> context.getPan();
            case "AMOUNT" -> context.getAmount();
            case "CURRENCY" -> context.getCurrencyCode();
            case "MTI", "MESSAGE_TYPE" -> context.getMti();
            case "MERCHANT_ID" -> context.getMerchantId();
            case "TERMINAL_ID" -> context.getTerminalId();
            case "SOURCE" -> context.getSource();
            case "PROTOCOL" -> context.getProtocol();
            case "COUNTRY" -> context.getCountryCode();
            default -> null;
        };
    }

    private Long safeParseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
