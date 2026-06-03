package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.service.ecommerce.EpgService;
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
@RequestMapping("/api/v1/epg")
@RequiredArgsConstructor
@Validated
public class EpgController {

    private final EpgService epgService;

    @PostMapping("/transactions")
    public ResponseEntity<EpgTransaction> initiateTransaction(@Valid @RequestBody Map<String, Object> request) {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.fromString((String) request.get("merchantId")),
                (String) request.get("merchantTransactionId"),
                new BigDecimal(request.get("amount").toString()),
                (String) request.get("currencyCode"));
        return ResponseEntity.ok(txn);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<EpgTransaction> getTransaction(@PathVariable UUID id) {
        EpgTransaction txn = epgService.getTransaction(id);
        if (txn == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(txn);
    }

    @GetMapping("/merchants/{merchantId}/transactions")
    public ResponseEntity<List<EpgTransaction>> getByMerchant(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(epgService.getTransactionsByMerchant(merchantId));
    }

    @PostMapping("/transactions/{id}/authorize")
    public ResponseEntity<EpgTransaction> authorize(@PathVariable UUID id,
                                                     @Valid @RequestBody Map<String, Object> request) {
        UUID cardId = request.get("cardId") != null
                ? UUID.fromString((String) request.get("cardId")) : null;
        EpgTransaction txn = epgService.authorizeTransaction(id, cardId,
                (String) request.get("cavv"), (String) request.get("eci"));
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/capture")
    public ResponseEntity<EpgTransaction> capture(@PathVariable UUID id) {
        EpgTransaction txn = epgService.captureTransaction(id);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/fail")
    public ResponseEntity<EpgTransaction> fail(@PathVariable UUID id,
                                                @Valid @RequestBody Map<String, String> request) {
        EpgTransaction txn = epgService.failTransaction(id,
                request.get("errorCode"), request.get("errorDescription"));
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/refund")
    public ResponseEntity<EpgTransaction> refund(@PathVariable UUID id) {
        EpgTransaction txn = epgService.refundTransaction(id);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/3ds")
    public ResponseEntity<EpgTransaction> setThreeDs(@PathVariable UUID id,
                                                      @Valid @RequestBody Map<String, Object> request) {
        EpgTransaction txn = epgService.setThreeDsStatus(id,
                (Boolean) request.get("required"),
                request.get("acsAuthId") != null
                        ? UUID.fromString((String) request.get("acsAuthId")) : null);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/merchants/{merchantId}/config")
    public ResponseEntity<EpgMerchantConfig> configureMerchant(@PathVariable UUID merchantId,
                                                                @Valid @RequestBody Map<String, String> request) {
        EpgMerchantConfig config = epgService.configureMerchant(merchantId,
                request.get("apiKeyHash"), request.get("apiSecretHash"));
        return ResponseEntity.ok(config);
    }

    @GetMapping("/merchants/{merchantId}/config")
    public ResponseEntity<EpgMerchantConfig> getMerchantConfig(@PathVariable UUID merchantId) {
        EpgMerchantConfig config = epgService.getMerchantConfig(merchantId);
        if (config == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/merchants")
    public ResponseEntity<List<EpgMerchantConfig>> getAllMerchantConfigs() {
        return ResponseEntity.ok(epgService.getAllMerchantConfigs());
    }

    @PostMapping("/merchants")
    public ResponseEntity<EpgMerchantConfig> createMerchantConfig(@Valid @RequestBody Map<String, Object> request) {
        EpgMerchantConfig config = epgService.createMerchantConfig(
                UUID.fromString((String) request.get("merchantId")),
                (String) request.get("apiKeyHash"),
                (String) request.get("apiSecretHash"),
                (String) request.get("webhookUrl"));
        return ResponseEntity.ok(config);
    }

    @PutMapping("/merchants/{id}")
    public ResponseEntity<EpgMerchantConfig> updateMerchantConfig(@PathVariable UUID id,
                                                                   @Valid @RequestBody Map<String, Object> request) {
        EpgMerchantConfig config = epgService.updateMerchantConfig(id,
                request.get("merchantId") != null ? UUID.fromString((String) request.get("merchantId")) : null,
                (String) request.get("apiKeyHash"),
                (String) request.get("apiSecretHash"),
                (String) request.get("webhookUrl"),
                request.get("isActive") != null ? Boolean.valueOf(request.get("isActive").toString()) : null);
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/merchants/{id}")
    public ResponseEntity<Void> deleteMerchantConfig(@PathVariable UUID id) {
        epgService.deleteMerchantConfig(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<EpgTransaction>> getAllTransactions() {
        return ResponseEntity.ok(epgService.getAllTransactions());
    }
}
