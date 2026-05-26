package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.service.ecommerce.EpgService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/epg")
@RequiredArgsConstructor
public class EpgController {

    private final EpgService epgService;

    @PostMapping("/transactions")
    public ResponseEntity<EpgTransaction> initiateTransaction(@RequestBody Map<String, Object> request) {
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
                                                     @RequestBody Map<String, String> request) {
        EpgTransaction txn = epgService.authorizeTransaction(id,
                request.get("cavv"), request.get("eci"));
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/capture")
    public ResponseEntity<EpgTransaction> capture(@PathVariable UUID id) {
        EpgTransaction txn = epgService.captureTransaction(id);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/transactions/{id}/fail")
    public ResponseEntity<EpgTransaction> fail(@PathVariable UUID id,
                                                @RequestBody Map<String, String> request) {
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
                                                       @RequestBody Map<String, Object> request) {
        EpgTransaction txn = epgService.setThreeDsStatus(id,
                (Boolean) request.get("required"),
                request.get("acsAuthId") != null
                        ? UUID.fromString((String) request.get("acsAuthId")) : null);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/merchants/{merchantId}/config")
    public ResponseEntity<EpgMerchantConfig> configureMerchant(@PathVariable UUID merchantId,
                                                                @RequestBody Map<String, String> request) {
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
}
