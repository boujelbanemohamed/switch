package com.switchplatform.platform.controller.simulator;

import com.switchplatform.platform.service.simulator.EcommerceFlowSimulator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulator")
@RequiredArgsConstructor
@Validated
public class SimulatorController {

    private final EcommerceFlowSimulator ecommerceFlowSimulator;

    @PostMapping("/ecommerce/frictionless")
    public ResponseEntity<EcommerceFlowSimulator.SimulationResult> simulateFrictionless(
            @Valid @RequestBody Map<String, Object> request) {

        EcommerceFlowSimulator.SimulationRequest simRequest = EcommerceFlowSimulator.SimulationRequest.builder()
                .cardId(UUID.fromString((String) request.get("cardId")))
                .merchantId(UUID.fromString((String) request.get("merchantId")))
                .amount(new BigDecimal(request.get("amount").toString()))
                .currencyCode((String) request.get("currencyCode"))
                .merchantTransactionId((String) request.get("merchantTransactionId"))
                .countryCode((String) request.get("countryCode"))
                .deviceId((String) request.get("deviceId"))
                .ipAddress((String) request.get("ipAddress"))
                .build();

        EcommerceFlowSimulator.SimulationResult result = ecommerceFlowSimulator.simulateFrictionlessFlow(simRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ecommerce/challenge")
    public ResponseEntity<EcommerceFlowSimulator.SimulationResult> simulateChallenge(
            @Valid @RequestBody Map<String, Object> request) {

        EcommerceFlowSimulator.SimulationRequest simRequest = EcommerceFlowSimulator.SimulationRequest.builder()
                .cardId(UUID.fromString((String) request.get("cardId")))
                .merchantId(UUID.fromString((String) request.get("merchantId")))
                .amount(new BigDecimal(request.get("amount").toString()))
                .currencyCode((String) request.get("currencyCode"))
                .merchantTransactionId((String) request.get("merchantTransactionId"))
                .countryCode((String) request.get("countryCode"))
                .deviceId((String) request.get("deviceId"))
                .ipAddress((String) request.get("ipAddress"))
                .build();

        EcommerceFlowSimulator.SimulationResult result = ecommerceFlowSimulator.simulateChallengeFlow(simRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ecommerce/challenge/verify")
    public ResponseEntity<EcommerceFlowSimulator.SimulationResult> verifyChallenge(
            @Valid @RequestBody Map<String, Object> request) {

        UUID challengeId = UUID.fromString((String) request.get("challengeId"));
        String otp = (String) request.get("otp");
        EcommerceFlowSimulator.SimulationResult result = ecommerceFlowSimulator.verifyChallengeFlow(challengeId, otp);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ecommerce/app-challenge")
    public ResponseEntity<EcommerceFlowSimulator.SimulationResult> simulateAppChallenge(
            @Valid @RequestBody Map<String, Object> request) {

        EcommerceFlowSimulator.SimulationRequest simRequest = EcommerceFlowSimulator.SimulationRequest.builder()
                .cardId(UUID.fromString((String) request.get("cardId")))
                .merchantId(UUID.fromString((String) request.get("merchantId")))
                .amount(new BigDecimal(request.get("amount").toString()))
                .currencyCode((String) request.get("currencyCode"))
                .merchantTransactionId((String) request.get("merchantTransactionId"))
                .countryCode((String) request.get("countryCode"))
                .deviceId((String) request.get("deviceId"))
                .ipAddress((String) request.get("ipAddress"))
                .build();

        EcommerceFlowSimulator.SimulationResult result = ecommerceFlowSimulator.simulateAppChallengeFlow(simRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ecommerce/app-challenge/respond")
    public ResponseEntity<EcommerceFlowSimulator.SimulationResult> respondAppChallenge(
            @Valid @RequestBody Map<String, Object> request) {

        UUID challengeId = UUID.fromString((String) request.get("challengeId"));
        String action = (String) request.get("action");
        EcommerceFlowSimulator.SimulationResult result = ecommerceFlowSimulator.respondToAppChallenge(challengeId, action);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ecommerce/batch")
    public ResponseEntity<EcommerceFlowSimulator.BatchResult> simulateBatch(
            @Valid @RequestBody Map<String, Object> request) {

        List<UUID> cardIds = new ArrayList<>();
        if (request.containsKey("cardIds")) {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) request.get("cardIds");
            for (String id : ids) cardIds.add(UUID.fromString(id));
        } else {
            cardIds.add(UUID.fromString((String) request.get("cardId")));
        }

        EcommerceFlowSimulator.BatchRequest batchRequest = EcommerceFlowSimulator.BatchRequest.builder()
                .totalTransactions((Integer) request.get("totalTransactions"))
                .frictionlessPercent((Integer) request.get("frictionlessPercent"))
                .challengePercent((Integer) request.get("challengePercent"))
                .appChallengePercent((Integer) request.get("appChallengePercent"))
                .declinedPercent((Integer) request.get("declinedPercent"))
                .cardIds(cardIds)
                .merchantId(UUID.fromString((String) request.get("merchantId")))
                .amountMin(new BigDecimal(request.get("amountMin").toString()))
                .amountMax(new BigDecimal(request.get("amountMax").toString()))
                .currencyCode((String) request.get("currencyCode"))
                .build();

        EcommerceFlowSimulator.BatchResult result = ecommerceFlowSimulator.simulateBatch(batchRequest);
        return ResponseEntity.ok(result);
    }
}
