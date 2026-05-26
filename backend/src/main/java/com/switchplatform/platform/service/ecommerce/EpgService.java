package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpgService {

    private final Map<UUID, EpgTransaction> transactions = new ConcurrentHashMap<>();
    private final Map<UUID, EpgMerchantConfig> merchantConfigs = new ConcurrentHashMap<>();

    public EpgTransaction initiateTransaction(UUID merchantId, String merchantTxnId,
                                                BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        boolean exists = transactions.values().stream()
                .anyMatch(t -> merchantId.equals(t.getMerchantId())
                        && merchantTxnId.equals(t.getMerchantTransactionId()));
        if (exists) {
            throw new IllegalArgumentException("Duplicate merchant transaction ID: " + merchantTxnId);
        }

        EpgTransaction txn = EpgTransaction.builder()
                .merchantId(merchantId)
                .merchantTransactionId(merchantTxnId)
                .amount(amount)
                .currencyCode(currencyCode)
                .status(EpgTransaction.Status.INITIATED)
                .deviceChannel(EpgTransaction.DeviceChannel.WEB)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        if (txn.getId() == null) {
            txn.setId(UUID.randomUUID());
        }

        transactions.put(txn.getId(), txn);
        log.info("EPG transaction initiated: id={}, merchant={}, txn={}, amount={}",
                txn.getId(), merchantId, merchantTxnId, amount);
        return txn;
    }

    public EpgTransaction getTransaction(UUID txnId) {
        return transactions.get(txnId);
    }

    public EpgTransaction getByMerchantTransaction(UUID merchantId, String merchantTxnId) {
        return transactions.values().stream()
                .filter(t -> merchantId.equals(t.getMerchantId())
                        && merchantTxnId.equals(t.getMerchantTransactionId()))
                .findFirst()
                .orElse(null);
    }

    public List<EpgTransaction> getTransactionsByMerchant(UUID merchantId) {
        return transactions.values().stream()
                .filter(t -> merchantId.equals(t.getMerchantId()))
                .sorted(Comparator.comparing(EpgTransaction::getCreatedAt).reversed())
                .toList();
    }

    public EpgTransaction authorizeTransaction(UUID txnId, String cavv, String eci) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setStatus(EpgTransaction.Status.AUTHORIZED);
        txn.setCavv(cavv);
        txn.setEci(eci);
        txn.setAuthorizedAt(OffsetDateTime.now());
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    public EpgTransaction captureTransaction(UUID txnId) {
        EpgTransaction txn = getOrThrow(txnId);
        if (txn.getStatus() != EpgTransaction.Status.AUTHORIZED) {
            throw new IllegalStateException("Transaction must be AUTHORIZED before capture: " + txnId);
        }
        txn.setStatus(EpgTransaction.Status.CAPTURED);
        txn.setCapturedAt(OffsetDateTime.now());
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    public EpgTransaction failTransaction(UUID txnId, String errorCode, String errorDescription) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setStatus(EpgTransaction.Status.FAILED);
        txn.setErrorCode(errorCode);
        txn.setErrorDescription(errorDescription);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    public EpgTransaction refundTransaction(UUID txnId) {
        EpgTransaction txn = getOrThrow(txnId);
        if (txn.getStatus() != EpgTransaction.Status.CAPTURED) {
            throw new IllegalStateException("Transaction must be CAPTURED before refund: " + txnId);
        }
        txn.setStatus(EpgTransaction.Status.REFUNDED);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    public EpgTransaction setThreeDsStatus(UUID txnId, Boolean required, UUID acsAuthId) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setThreeDsRequired(required);
        txn.setAcsTransactionId(acsAuthId);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    public EpgMerchantConfig configureMerchant(UUID merchantId, String apiKeyHash, String apiSecretHash) {
        EpgMerchantConfig config = EpgMerchantConfig.builder()
                .merchantId(merchantId)
                .apiKeyHash(apiKeyHash)
                .apiSecretHash(apiSecretHash)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        if (config.getId() == null) {
            config.setId(UUID.randomUUID());
        }

        merchantConfigs.put(config.getId(), config);
        return config;
    }

    public EpgMerchantConfig getMerchantConfig(UUID merchantId) {
        return merchantConfigs.values().stream()
                .filter(c -> merchantId.equals(c.getMerchantId()))
                .findFirst()
                .orElse(null);
    }

    private EpgTransaction getOrThrow(UUID txnId) {
        EpgTransaction txn = transactions.get(txnId);
        if (txn == null) {
            throw new IllegalArgumentException("Transaction not found: " + txnId);
        }
        return txn;
    }
}
