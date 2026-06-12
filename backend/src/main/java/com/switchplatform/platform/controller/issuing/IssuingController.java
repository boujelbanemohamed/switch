package com.switchplatform.platform.controller.issuing;

import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.model.issuing.Cardholder;
import com.switchplatform.platform.model.issuing.WalletToken;
import com.switchplatform.platform.service.issuing.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issuing")
@RequiredArgsConstructor
@Slf4j
@Validated
public class IssuingController {

    private final CardholderService cardholderService;
    private final CardService cardService;
    private final WalletTokenService walletTokenService;
    private final CardAccountService cardAccountService;
    private final IssuingNotificationService notificationService;
    private final PinService pinService;
    private final TokenVaultService tokenVaultService;

    // ─── Cardholder endpoints ───────────────────────────────────────────────

    @GetMapping("/cardholders")
    public ResponseEntity<?> listCardholders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(cardholderService.listAll(page, size));
    }

    @PostMapping("/cardholders")
    public ResponseEntity<Cardholder> createCardholder(@Valid @RequestBody Cardholder cardholder) {
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
    public ResponseEntity<Cardholder> updateCardholder(@PathVariable UUID id, @Valid @RequestBody Cardholder cardholder) {
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
    public ResponseEntity<CreateCardResponse> createCard(@Valid @RequestBody CreateCardRequest req) {
        Card card = new Card();
        card.setCardholderId(req.getCardholderId());
        CardService.CreateCardResult result = cardService.createCardWithRawValues(card);
        return ResponseEntity.ok(CreateCardResponse.from(result.card(), result.rawCardNumber(), result.rawCvv()));
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
    public ResponseEntity<Card> blockCard(@PathVariable UUID id, @Valid @RequestBody Map<String, String> body) {
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
    public ResponseEntity<Card> updateLimits(@PathVariable UUID id, @Valid @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(cardService.updateCardLimits(
                id,
                body.get("dailyLimit"),
                body.get("weeklyLimit"),
                body.get("monthlyLimit"),
                body.get("singleTxnLimit")
        ));
    }

    @PutMapping("/cards/{id}/pin")
    public ResponseEntity<Card> changePin(@PathVariable UUID id, @Valid @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cardService.changePin(id, body.get("pinBlock")));
    }

    @PostMapping("/cards/{id}/pin/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPin(@PathVariable UUID id, @Valid @RequestBody Map<String, String> body) {
        boolean matches = cardService.verifyPin(id, body.get("pinBlock"));
        return ResponseEntity.ok(Map.of("verified", matches));
    }

    @GetMapping("/cardholders/{cardholderId}/cards")
    public ResponseEntity<List<Card>> getCardsForCardholder(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(cardService.getCardsByCardholderId(cardholderId));
    }

    // ─── Wallet token endpoints ─────────────────────────────────────────────

    @PostMapping("/tokens")
    public ResponseEntity<WalletToken> tokenizeCard(@Valid @RequestBody Map<String, String> body) {
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

    // ─── Card Account endpoints ─────────────────────────────────────────────

    @PostMapping("/accounts")
    public ResponseEntity<CardAccount> createAccount(@Valid @RequestBody CardAccount account) {
        return ResponseEntity.ok(cardAccountService.createAccount(account));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<CardAccount> getAccount(@PathVariable UUID id) {
        return cardAccountService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cardholders/{cardholderId}/accounts")
    public ResponseEntity<List<CardAccount>> getAccountsByCardholder(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(cardAccountService.getAccountsByCardholderId(cardholderId));
    }

    @PostMapping("/accounts/{id}/debit")
    public ResponseEntity<CardAccount> debitAccount(
            @PathVariable UUID id, @Valid @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.get("currencyCode");
        return ResponseEntity.ok(cardAccountService.debit(id, amount, currency));
    }

    @PostMapping("/accounts/{id}/credit")
    public ResponseEntity<CardAccount> creditAccount(
            @PathVariable UUID id, @Valid @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.get("currencyCode");
        return ResponseEntity.ok(cardAccountService.credit(id, amount, currency));
    }

    @PostMapping("/accounts/{id}/hold")
    public ResponseEntity<CardAccount> holdAccount(
            @PathVariable UUID id, @Valid @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return ResponseEntity.ok(cardAccountService.hold(id, amount));
    }

    @PostMapping("/accounts/{id}/release-hold")
    public ResponseEntity<CardAccount> releaseHold(
            @PathVariable UUID id, @Valid @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return ResponseEntity.ok(cardAccountService.releaseHold(id, amount));
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(cardAccountService.listAll(page, size));
    }

    // ─── Notification endpoints ─────────────────────────────────────────────

    @GetMapping("/notifications")
public ResponseEntity<List<IssuingNotificationService.Notification>> listNotifications() {
        return ResponseEntity.ok(notificationService.listAll());
    }

    @GetMapping("/notifications/by-cardholder/{cardholderId}")
    public ResponseEntity<List<IssuingNotificationService.Notification>> getNotificationsByCardholder(
            @PathVariable UUID cardholderId) {
        return ResponseEntity.ok(notificationService.getNotificationsByCardholder(cardholderId));
    }

    @GetMapping("/notifications/by-card/{cardId}")
    public ResponseEntity<List<IssuingNotificationService.Notification>> getNotificationsByCard(
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(notificationService.getNotificationsByCard(cardId));
    }

    // ─── PIN Vault endpoints ────────────────────────────────────────────────

    @PostMapping("/pins")
    public ResponseEntity<Map<String, String>> createPin(@Valid @RequestBody Map<String, String> body) {
        String cardId = body.get("cardId");
        String rawPin = body.get("rawPin");
        String pinBlock = body.get("pinBlock");
        String message = pinService.createPin(cardId, rawPin, pinBlock);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/pins/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPinVault(@Valid @RequestBody Map<String, String> body) {
        String cardId = body.get("cardId");
        String pinBlock = body.get("pinBlock");
        boolean verified = pinService.verifyPin(cardId, pinBlock);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PutMapping("/pins")
    public ResponseEntity<Map<String, Boolean>> changePinVault(@Valid @RequestBody Map<String, String> body) {
        String cardId = body.get("cardId");
        String oldPinBlock = body.get("oldPinBlock");
        String newPinBlock = body.get("newPinBlock");
        boolean changed = pinService.changePin(cardId, oldPinBlock, newPinBlock);
        return ResponseEntity.ok(Map.of("changed", changed));
    }

    // ─── Token Vault endpoints ──────────────────────────────────────────────

    @PostMapping("/tokens/tokenize")
    public ResponseEntity<WalletToken> tokenize(@Valid @RequestBody Map<String, String> body) {
        String cardId = body.get("cardId");
        String walletProvider = body.get("walletProvider");
        String deviceId = body.get("deviceId");
        String deviceName = body.get("deviceName");

        WalletToken token = tokenVaultService.storeToken(
                UUID.fromString(cardId), null, walletProvider, deviceId, deviceName);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/tokens/by-dpan/{dpan}")
    public ResponseEntity<WalletToken> getByDpan(@PathVariable String dpan) {
        return tokenVaultService.getTokenByDpan(dpan)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tokens/{dpan}/suspend")
    public ResponseEntity<Void> suspendTokenVault(@PathVariable String dpan) {
        tokenVaultService.revokeToken(dpan);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tokens/by-card/{cardId}")
    public ResponseEntity<List<WalletToken>> listByCard(@PathVariable String cardId) {
        return ResponseEntity.ok(tokenVaultService.getTokensForCard(UUID.fromString(cardId)));
    }
}
