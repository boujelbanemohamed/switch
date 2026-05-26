package com.switch.platform.controller.issuing;

import com.switch.platform.model.issuing.Card;
import com.switch.platform.model.issuing.Cardholder;
import com.switch.platform.model.issuing.WalletToken;
import com.switch.platform.service.issuing.CardService;
import com.switch.platform.service.issuing.CardholderService;
import com.switch.platform.service.issuing.WalletTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issuing")
@RequiredArgsConstructor
@Slf4j
public class IssuingController {

    private final CardholderService cardholderService;
    private final CardService cardService;
    private final WalletTokenService walletTokenService;

    // ─── Cardholder endpoints ───────────────────────────────────────────────

    @PostMapping("/cardholders")
    public ResponseEntity<Cardholder> createCardholder(@RequestBody Cardholder cardholder) {
        return ResponseEntity.ok(cardholderService.createCardholder(cardholder));
    }

    @GetMapping("/cardholders/{id}")
    public ResponseEntity<Cardholder> getCardholder(@PathVariable UUID id) {
        return cardholderService.getCardholder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cardholders/by-email/{email}")
    public ResponseEntity<Cardholder> getCardholderByEmail(@PathVariable String email) {
        return cardholderService.getCardholderByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/cardholders/{id}")
    public ResponseEntity<Cardholder> updateCardholder(@PathVariable UUID id, @RequestBody Cardholder cardholder) {
        return ResponseEntity.ok(cardholderService.updateCardholder(id, cardholder));
    }

    @PutMapping("/cardholders/{id}/kyc")
    public ResponseEntity<Cardholder> updateKycLevel(@PathVariable UUID id, @RequestParam int level) {
        return ResponseEntity.ok(cardholderService.updateKycLevel(id, level));
    }

    @PostMapping("/cardholders/{id}/block")
    public ResponseEntity<Cardholder> blockCardholder(@PathVariable UUID id) {
        return ResponseEntity.ok(cardholderService.blockCardholder(id));
    }

    // ─── Card endpoints ─────────────────────────────────────────────────────

    @PostMapping("/cards")
    public ResponseEntity<Card> createCard(@RequestBody Card card) {
        return ResponseEntity.ok(cardService.createCard(card));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<Card> getCard(@PathVariable UUID id) {
        return cardService.getCard(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cards/by-suffix/{suffix}")
    public ResponseEntity<Card> findCardBySuffix(@PathVariable String suffix) {
        return cardService.getCardBySuffix(suffix)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cards/{id}/activate")
    public ResponseEntity<Card> activateCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @PostMapping("/cards/{id}/block")
    public ResponseEntity<Card> blockCard(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "BLOCKED");
        return ResponseEntity.ok(cardService.blockCard(id, reason));
    }

    @PostMapping("/cards/{id}/unblock")
    public ResponseEntity<Card> unblockCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.unblockCard(id));
    }

    @PostMapping("/cards/{id}/report-lost")
    public ResponseEntity<Card> reportLost(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.reportLost(id));
    }

    @PostMapping("/cards/{id}/report-stolen")
    public ResponseEntity<Card> reportStolen(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.reportStolen(id));
    }

    @PostMapping("/cards/{id}/renew")
    public ResponseEntity<Card> renewCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.renewCard(id));
    }

    @PutMapping("/cards/{id}/limits")
    public ResponseEntity<Card> updateLimits(@PathVariable UUID id, @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(cardService.updateCardLimits(
                id,
                body.get("dailyLimit"),
                body.get("weeklyLimit"),
                body.get("monthlyLimit"),
                body.get("singleTxnLimit")
        ));
    }

    @PutMapping("/cards/{id}/pin")
    public ResponseEntity<Card> changePin(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cardService.changePin(id, body.get("pinBlock")));
    }

    @PostMapping("/cards/{id}/pin/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPin(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        boolean matches = cardService.verifyPin(id, body.get("pinBlock"));
        return ResponseEntity.ok(Map.of("verified", matches));
    }

    @GetMapping("/cardholders/{cardholderId}/cards")
    public ResponseEntity<List<Card>> getCardsForCardholder(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(cardService.getCardsByCardholderId(cardholderId));
    }

    // ─── Wallet token endpoints ─────────────────────────────────────────────

    @PostMapping("/tokens")
    public ResponseEntity<WalletToken> tokenizeCard(@RequestBody Map<String, String> body) {
        UUID cardId = UUID.fromString(body.get("cardId"));
        return ResponseEntity.ok(walletTokenService.tokenizeCard(
                cardId,
                body.get("walletProvider"),
                body.get("deviceId")
        ));
    }

    @GetMapping("/tokens/{tokenUuid}")
    public ResponseEntity<WalletToken> getToken(@PathVariable UUID tokenUuid) {
        return walletTokenService.getToken(tokenUuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tokens/{token}/suspend")
    public ResponseEntity<WalletToken> suspendToken(@PathVariable String token) {
        return walletTokenService.suspendToken(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tokens/{token}/terminate")
    public ResponseEntity<WalletToken> terminateToken(@PathVariable String token) {
        return walletTokenService.terminateToken(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cards/{cardId}/tokens")
    public ResponseEntity<List<WalletToken>> getTokensForCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(walletTokenService.getTokensByCard(cardId));
    }
}
