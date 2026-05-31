package com.switchplatform.platform.controller.fraud;

import com.switchplatform.platform.model.fraud.BehavioralProfile;
import com.switchplatform.platform.model.fraud.DeviceFingerprintRecord;
import com.switchplatform.platform.model.fraud.FraudAlert;
import com.switchplatform.platform.model.fraud.FraudRule;
import com.switchplatform.platform.service.fraud.BehavioralProfileService;
import com.switchplatform.platform.service.fraud.DeviceFingerprintService;
import com.switchplatform.platform.service.fraud.DeviceScoreResult;
import com.switchplatform.platform.service.fraud.FraudEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Validated
public class FraudController {

    private final FraudEngine fraudEngine;
    private final BehavioralProfileService behavioralProfileService;
    private final DeviceFingerprintService deviceFingerprintService;

    @PostMapping("/evaluate")
    public ResponseEntity<FraudEngine.FraudEvaluationResult> evaluate(
            @Valid @RequestBody FraudEngine.EvaluationContext context) {
        return ResponseEntity.ok(fraudEngine.evaluateTransaction(context));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<FraudRule>> listRules() {
        return ResponseEntity.ok(fraudEngine.listRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<FraudRule> defineRule(@Valid @RequestBody FraudRule rule) {
        return ResponseEntity.ok(fraudEngine.defineRule(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        fraudEngine.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlert>> listAlerts(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(fraudEngine.getAlertsByStatus(status));
        }
        return ResponseEntity.ok(fraudEngine.getAlertsByStatus("OPEN"));
    }

    @GetMapping("/alerts/all")
    public ResponseEntity<List<FraudAlert>> getAllAlerts() {
        return ResponseEntity.ok(fraudEngine.getAllAlerts());
    }

    @GetMapping("/alerts/stats")
    public ResponseEntity<Map<String, Long>> getAlertStats() {
        return ResponseEntity.ok(fraudEngine.getAlertStats());
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

    @GetMapping("/profiles")
    public ResponseEntity<List<BehavioralProfile>> listProfiles() {
        return ResponseEntity.ok(behavioralProfileService.listAllProfiles());
    }

    @GetMapping("/profiles/{cardholderId}")
    public ResponseEntity<BehavioralProfile> getProfile(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(behavioralProfileService.getOrCreateProfile(cardholderId));
    }

    @PostMapping("/profiles/{cardholderId}/record")
    public ResponseEntity<Void> recordTransaction(
            @PathVariable UUID cardholderId,
            @Valid @RequestBody BehavioralProfileService.TransactionRequest request) {
        behavioralProfileService.recordTransaction(cardholderId,
                request.getMerchantCategory(),
                request.getCountryCode(),
                request.getAmount(),
                request.getTimestamp());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profiles/{cardholderId}/anomalies")
    public ResponseEntity<BehavioralProfileService.AnomalyResult> checkAnomalies(
            @PathVariable UUID cardholderId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String merchantCategory,
            @RequestParam(required = false) String countryCode) {
        return ResponseEntity.ok(behavioralProfileService.detectAnomalies(
                cardholderId, amount, merchantCategory, countryCode, null));
    }

    @PostMapping("/devices/register")
    public ResponseEntity<DeviceFingerprintRecord> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request) {
        return ResponseEntity.ok(deviceFingerprintService.registerFingerprint(
                request.cardId(), request.deviceId(), request.deviceType(),
                request.os(), request.browser(), request.userAgent(),
                request.ipAddress(), request.attributes()));
    }

    @PostMapping("/devices/evaluate")
    public ResponseEntity<DeviceScoreResult> evaluateDevice(
            @Valid @RequestBody EvaluateDeviceRequest request) {
        return ResponseEntity.ok(deviceFingerprintService.evaluate(
                request.cardId(), request.deviceId(), request.deviceType(),
                request.os(), request.browser(), request.userAgent(),
                request.ipAddress()));
    }

    @GetMapping("/devices/by-card/{cardId}")
    public ResponseEntity<List<DeviceFingerprintRecord>> getDevicesByCard(
            @PathVariable String cardId) {
        return ResponseEntity.ok(deviceFingerprintService.getFingerprintsForCard(cardId));
    }

    @GetMapping("/devices/{deviceId}/known")
    public ResponseEntity<Map<String, Boolean>> isDeviceKnown(
            @PathVariable String deviceId,
            @RequestParam String cardId) {
        boolean known = deviceFingerprintService.isKnownDevice(cardId, deviceId);
        return ResponseEntity.ok(Map.of("known", known));
    }

    public record RegisterDeviceRequest(
            String cardId, String deviceId, String deviceType,
            String os, String browser, String userAgent,
            String ipAddress, Map<String, String> attributes) {}

    public record EvaluateDeviceRequest(
            String cardId, String deviceId, String deviceType,
            String os, String browser, String userAgent,
            String ipAddress) {}
}
