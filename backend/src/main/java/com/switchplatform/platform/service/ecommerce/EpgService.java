package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.repository.ecommerce.EpgMerchantConfigRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpgService {

    private final EpgTransactionRepository epgTransactionRepository;
    private final EpgMerchantConfigRepository epgMerchantConfigRepository;

    @Transactional
    public EpgTransaction initiateTransaction(UUID merchantId, String merchantTxnId,
                                                BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (epgTransactionRepository.findByMerchantIdAndMerchantTransactionId(merchantId, merchantTxnId).isPresent()) {
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

        epgTransactionRepository.save(txn);
        log.info("EPG transaction initiated: id={}, merchant={}, txn={}, amount={}",
                txn.getId(), merchantId, merchantTxnId, amount);
        return txn;
    }

    @Transactional(readOnly = true)
    public EpgTransaction getTransaction(UUID txnId) {
        return epgTransactionRepository.findById(txnId).orElse(null);
    }

    @Transactional(readOnly = true)
    public EpgTransaction getByMerchantTransaction(UUID merchantId, String merchantTxnId) {
        return epgTransactionRepository.findByMerchantIdAndMerchantTransactionId(merchantId, merchantTxnId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EpgTransaction> getTransactionsByMerchant(UUID merchantId) {
        return epgTransactionRepository.findByMerchantId(merchantId).stream()
                .sorted(Comparator.comparing(EpgTransaction::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public EpgTransaction authorizeTransaction(UUID txnId, String cavv, String eci) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setStatus(EpgTransaction.Status.AUTHORIZED);
        txn.setCavv(cavv);
        txn.setEci(eci);
        txn.setAuthorizedAt(OffsetDateTime.now());
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    @Transactional
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

    @Transactional
    public EpgTransaction failTransaction(UUID txnId, String errorCode, String errorDescription) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setStatus(EpgTransaction.Status.FAILED);
        txn.setErrorCode(errorCode);
        txn.setErrorDescription(errorDescription);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    @Transactional
    public EpgTransaction refundTransaction(UUID txnId) {
        EpgTransaction txn = getOrThrow(txnId);
        if (txn.getStatus() != EpgTransaction.Status.CAPTURED) {
            throw new IllegalStateException("Transaction must be CAPTURED before refund: " + txnId);
        }
        txn.setStatus(EpgTransaction.Status.REFUNDED);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    @Transactional
    public EpgTransaction setThreeDsStatus(UUID txnId, Boolean required, UUID acsAuthId) {
        EpgTransaction txn = getOrThrow(txnId);
        txn.setThreeDsRequired(required);
        txn.setAcsTransactionId(acsAuthId);
        txn.setUpdatedAt(OffsetDateTime.now());
        return txn;
    }

    @Transactional
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

        epgMerchantConfigRepository.save(config);
        return config;
    }

    @Transactional(readOnly = true)
    public EpgMerchantConfig getMerchantConfig(UUID merchantId) {
        return epgMerchantConfigRepository.findByMerchantId(merchantId).orElse(null);
    }

    private EpgTransaction getOrThrow(UUID txnId) {
        return epgTransactionRepository.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txnId));
    }
}
