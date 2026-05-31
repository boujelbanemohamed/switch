package com.switchplatform.platform.controller.authorization;

import com.switchplatform.platform.model.authorization.AuthDecision;
import com.switchplatform.platform.model.authorization.AuthRule;
import com.switchplatform.platform.model.authorization.HoldRecord;
import com.switchplatform.platform.service.authorization.AuthorizationEngine;
import com.switchplatform.platform.service.authorization.HoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authorization")
@RequiredArgsConstructor
@Validated
public class AuthorizationController {

    private final AuthorizationEngine authorizationEngine;
    private final HoldService holdService;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationEngine.AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationEngine.AuthorizationRequest request) {
        return ResponseEntity.ok(authorizationEngine.authorize(request));
    }

    @PostMapping("/rules")
    public ResponseEntity<AuthRule> defineRule(@Valid @RequestBody AuthRule rule) {
        return ResponseEntity.ok(authorizationEngine.defineRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AuthRule> updateRule(@PathVariable UUID id, @Valid @RequestBody AuthRule rule) {
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

    @GetMapping("/holds/card/{cardId}")
    public ResponseEntity<List<HoldRecord>> getHoldsForCard(@PathVariable String cardId) {
        return ResponseEntity.ok(holdService.getActiveHoldsForCard(cardId));
    }

    @GetMapping("/holds/account/{accountId}")
    public ResponseEntity<List<HoldRecord>> getHoldsForAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(holdService.getActiveHoldsForAccount(accountId));
    }

    @PostMapping("/holds/{holdId}/release")
    public ResponseEntity<Void> releaseHold(@PathVariable UUID holdId) {
        boolean released = holdService.releaseHold(holdId);
        if (!released) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/holds/{holdId}/capture")
    public ResponseEntity<Void> captureHold(@PathVariable UUID holdId) {
        boolean captured = holdService.captureHold(holdId);
        if (!captured) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
}
