package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import com.switchplatform.platform.service.ecommerce.AcsService;
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
@RequestMapping("/api/v1/acs")
@RequiredArgsConstructor
@Validated
public class AcsController {

    private final AcsService acsService;

    @GetMapping("/authentications")
    public ResponseEntity<List<AcsAuthentication>> getAllAuthentications() {
        return ResponseEntity.ok(acsService.getAllAuthentications());
    }

    @PostMapping("/authentications")
    public ResponseEntity<AcsAuthentication> createAuthentication(@Valid @RequestBody Map<String, Object> request) {
        UUID cardId = request.get("cardId") != null
                ? UUID.fromString((String) request.get("cardId")) : null;
        UUID merchantId = request.get("merchantId") != null
                ? UUID.fromString((String) request.get("merchantId")) : null;
        AcsAuthentication auth = acsService.createAuthentication(
                (String) request.get("transactionId"),
                cardId, merchantId,
                new BigDecimal(request.get("amount").toString()),
                (String) request.get("currencyCode"));
        return ResponseEntity.ok(auth);
    }

    @GetMapping("/authentications/{id}")
    public ResponseEntity<AcsAuthentication> getAuthentication(@PathVariable UUID id) {
        AcsAuthentication auth = acsService.getAuthentication(id);
        if (auth == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(auth);
    }

    @GetMapping("/authentications/by-card/{cardId}")
    public ResponseEntity<List<AcsAuthentication>> getByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(acsService.getAuthenticationsByCard(cardId));
    }

    @PostMapping("/authentications/{id}/challenge")
    public ResponseEntity<AcsAuthentication> requestChallenge(@PathVariable UUID id) {
        AcsAuthentication auth = acsService.requestChallenge(id);
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/authentications/{id}/authenticate")
    public ResponseEntity<AcsAuthentication> authenticate(@PathVariable UUID id,
                                                           @Valid @RequestBody Map<String, String> request) {
        AcsAuthentication auth = acsService.authenticate(id,
                request.get("authenticationValue"), request.get("eci"));
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/authentications/{id}/fail")
    public ResponseEntity<AcsAuthentication> failAuthentication(@PathVariable UUID id) {
        AcsAuthentication auth = acsService.failAuthentication(id);
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/challenges")
    public ResponseEntity<AcsChallenge> createChallenge(@Valid @RequestBody Map<String, Object> request) {
        AcsChallenge challenge = acsService.createChallenge(
                UUID.fromString((String) request.get("authenticationId")),
                AcsChallenge.ChallengeType.valueOf((String) request.get("challengeType")));
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/challenges/{id}/verify")
    public ResponseEntity<AcsChallenge> verifyChallenge(@PathVariable UUID id) {
        AcsChallenge challenge = acsService.verifyChallenge(id);
        return ResponseEntity.ok(challenge);
    }

    @GetMapping("/challenges/{id}")
    public ResponseEntity<AcsChallenge> getChallenge(@PathVariable UUID id) {
        AcsChallenge challenge = acsService.getChallenge(id);
        if (challenge == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(challenge);
    }

    @GetMapping("/authentications/{authId}/challenges")
    public ResponseEntity<List<AcsChallenge>> getChallengesByAuth(@PathVariable UUID authId) {
        return ResponseEntity.ok(acsService.getChallengesByAuth(authId));
    }

    @PostMapping("/authentications/{id}/evaluate-rba")
    public ResponseEntity<AcsAuthentication> evaluateRba(@PathVariable UUID id,
                                                          @RequestBody(required = false) Map<String, Object> context) {
        return ResponseEntity.ok(acsService.evaluateRba(id, context));
    }
}
