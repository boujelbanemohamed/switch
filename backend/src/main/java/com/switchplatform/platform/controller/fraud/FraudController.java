package com.switchplatform.platform.controller.fraud;

import com.switchplatform.platform.model.fraud.FraudAlert;
import com.switchplatform.platform.model.fraud.FraudRule;
import com.switchplatform.platform.service.fraud.FraudEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudEngine fraudEngine;

    @PostMapping("/evaluate")
    public ResponseEntity<FraudEngine.FraudEvaluationResult> evaluate(
            @RequestBody FraudEngine.EvaluationContext context) {
        return ResponseEntity.ok(fraudEngine.evaluateTransaction(context));
    }

    @PostMapping("/rules")
    public ResponseEntity<FraudRule> defineRule(@RequestBody FraudRule rule) {
        return ResponseEntity.ok(fraudEngine.defineRule(rule));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlert>> listAlerts(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(fraudEngine.getAlertsByStatus(status));
        }
        return ResponseEntity.ok(fraudEngine.getAlertsByStatus("OPEN"));
    }

    @GetMapping("/alerts/card/{cardId}")
    public ResponseEntity<List<FraudAlert>> getAlertsByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(fraudEngine.getAlertsByCard(cardId));
    }

    @PutMapping("/alerts/{id}/status")
    public ResponseEntity<FraudAlert> updateAlertStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam String decision) {
        return ResponseEntity.ok(fraudEngine.updateAlertStatus(id, status, decision));
    }

    @PostMapping("/alerts/{id}/confirm")
    public ResponseEntity<FraudAlert> confirmFraud(@PathVariable UUID id) {
        return ResponseEntity.ok(fraudEngine.confirmFraud(id));
    }

    @PostMapping("/alerts/{id}/dismiss")
    public ResponseEntity<FraudAlert> dismissAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(fraudEngine.dismissAsFalsePositive(id));
    }
}
