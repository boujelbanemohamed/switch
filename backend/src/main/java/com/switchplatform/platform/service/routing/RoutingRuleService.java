package com.switchplatform.platform.service.routing;

import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;

    public List<RoutingRule> findAll() {
        return routingRuleRepository.findAll();
    }

    public RoutingRule findById(UUID id) {
        return routingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Routing rule not found: " + id));
    }

    public RoutingRule create(RoutingRule rule) {
        return routingRuleRepository.save(rule);
    }

    public RoutingRule update(UUID id, RoutingRule rule) {
        RoutingRule existing = findById(id);
        if (rule.getName() != null) existing.setName(rule.getName());
        if (rule.getDescription() != null) existing.setDescription(rule.getDescription());
        if (rule.getSourceParticipant() != null) existing.setSourceParticipant(rule.getSourceParticipant());
        if (rule.getDestinationParticipant() != null) existing.setDestinationParticipant(rule.getDestinationParticipant());
        if (rule.getProtocol() != null) existing.setProtocol(rule.getProtocol());
        if (rule.getPriority() != null) existing.setPriority(rule.getPriority());
        if (rule.getStatus() != null) existing.setStatus(rule.getStatus());
        if (rule.getConditionExpression() != null) existing.setConditionExpression(rule.getConditionExpression());
        return routingRuleRepository.save(existing);
    }

    public void delete(UUID id) {
        routingRuleRepository.deleteById(id);
    }
}
