package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import com.switchplatform.platform.service.ecommerce.ThreeDsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/3dss")
@RequiredArgsConstructor
@Validated
public class ThreeDsController {

    private final ThreeDsService threeDsService;

    @PostMapping("/sessions")
    public ResponseEntity<ThreeDsSession> createSession(@Valid @RequestBody Map<String, Object> request) {
        ThreeDsSession session = threeDsService.createSession(
                (String) request.get("transactionId"),
                request.get("epgTransactionId") != null
                        ? UUID.fromString((String) request.get("epgTransactionId")) : null,
                request.get("cardId") != null
                        ? UUID.fromString((String) request.get("cardId")) : null,
                (String) request.get("notificationUrl"));
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ThreeDsSession> getSession(@PathVariable UUID id) {
        ThreeDsSession session = threeDsService.getSession(id);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/by-txn/{transactionId}")
    public ResponseEntity<ThreeDsSession> getByTransactionId(@PathVariable String transactionId) {
        ThreeDsSession session = threeDsService.getSessionByTransactionId(transactionId);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/auth-req")
    public ResponseEntity<ThreeDsSession> sendAuthRequest(@PathVariable UUID id,
                                                           @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.sendAuthenticationRequest(id,
                request.get("acsUrl"), request.get("creq"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/auth-res")
    public ResponseEntity<ThreeDsSession> receiveAuthResponse(@PathVariable UUID id,
                                                                @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.receiveAuthenticationResponse(id,
                request.get("acsTransId"), request.get("dsTransId"),
                request.get("authenticationValue"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/challenge")
    public ResponseEntity<ThreeDsSession> initiateChallenge(@PathVariable UUID id,
                                                             @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.initiateChallenge(id,
                request.get("challengeRequest"), request.get("acsUrl"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/challenge-res")
    public ResponseEntity<ThreeDsSession> completeChallenge(@PathVariable UUID id,
                                                             @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.completeChallenge(id,
                request.get("challengeResponse"), request.get("cres"),
                request.get("eci"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<ThreeDsSession> completeSession(@PathVariable UUID id,
                                                           @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.completeSession(id,
                request.get("authenticationValue"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/fail")
    public ResponseEntity<ThreeDsSession> failSession(@PathVariable UUID id,
                                                        @Valid @RequestBody Map<String, String> request) {
        ThreeDsSession session = threeDsService.failSession(id,
                request.get("errorDescription"));
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ThreeDsSession>> getAllSessions() {
        return ResponseEntity.ok(threeDsService.getAllSessions());
    }

    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<ThreeDsSession> cancelSession(@PathVariable UUID id) {
        return ResponseEntity.ok(threeDsService.cancelSession(id));
    }
}
