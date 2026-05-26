package com.switch.platform.controller.authorization;

import com.switch.platform.model.authorization.AuthDecision;
import com.switch.platform.model.authorization.AuthRule;
import com.switch.platform.service.authorization.AuthorizationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationEngine authorizationEngine;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationEngine.AuthorizationResponse> authorize(
            @RequestBody AuthorizationEngine.AuthorizationRequest request) {
        return ResponseEntity.ok(authorizationEngine.authorize(request));
    }

    @PostMapping("/rules")
    public ResponseEntity<AuthRule> defineRule(@RequestBody AuthRule rule) {
        return ResponseEntity.ok(authorizationEngine.defineRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AuthRule> updateRule(@PathVariable UUID id, @RequestBody AuthRule rule) {
        return ResponseEntity.ok(authorizationEngine.updateRule(id, rule));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AuthRule>> listRules() {
        return ResponseEntity.ok(authorizationEngine.getRules());
    }

    @GetMapping("/decisions/{cardId}")
    public ResponseEntity<List<AuthDecision>> getDecisions(
            @PathVariable UUID cardId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(authorizationEngine.getDecisions(cardId, limit));
    }

    @GetMapping("/decisions/by-transaction/{transactionId}")
    public ResponseEntity<AuthDecision> getDecisionByTransactionId(@PathVariable String transactionId) {
        AuthDecision decision = authorizationEngine.getDecisionByTransactionId(transactionId);
        if (decision == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(decision);
    }
}
