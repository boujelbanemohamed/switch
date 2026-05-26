package com.switchplatform.platform.controller;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.service.MonitoringService;
import com.switchplatform.platform.service.SwitchCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/switch")
@RequiredArgsConstructor
@Slf4j
public class SwitchController {

    private final SwitchCore switchCore;
    private final MonitoringService monitoringService;

    @PostMapping(value = "/iso8583", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, Object>> processIso8583(
            @RequestBody byte[] message,
            @RequestHeader("X-Source-Code") String sourceCode) {
        log.info("Received ISO 8583 message from {} ({} bytes)", sourceCode, message.length);

        Transaction transaction = switchCore.processIso8583Message(message, sourceCode);

        return ResponseEntity.ok(Map.of(
                "transactionId", transaction.getTransactionId(),
                "status", transaction.getStatus(),
                "responseCode", transaction.getResponseCode() != null ?
                        transaction.getResponseCode() : ""
        ));
    }

    @PostMapping("/iso8583/base64")
    public ResponseEntity<Map<String, Object>> processIso8583Base64(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Source-Code") String sourceCode) {
        byte[] message = Base64.getDecoder().decode(request.get("message"));
        Transaction transaction = switchCore.processIso8583Message(message, sourceCode);

        return ResponseEntity.ok(Map.of(
                "transactionId", transaction.getTransactionId(),
                "status", transaction.getStatus(),
                "responseCode", transaction.getResponseCode() != null ?
                        transaction.getResponseCode() : ""
        ));
    }

    @PostMapping("/iso20022")
    public ResponseEntity<Map<String, Object>> processIso20022(
            @RequestBody String xmlMessage,
            @RequestHeader("X-Source-Code") String sourceCode) {
        log.info("Received ISO 20022 message from {}", sourceCode);

        Transaction transaction = switchCore.processIso20022Message(xmlMessage, sourceCode);

        return ResponseEntity.ok(Map.of(
                "transactionId", transaction.getTransactionId(),
                "status", transaction.getStatus(),
                "responseCode", transaction.getResponseCode() != null ?
                        transaction.getResponseCode() : ""
        ));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable String transactionId) {
        return monitoringService.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(monitoringService.getRecentTransactions(page, size));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "switch-platform"
        ));
    }
}
