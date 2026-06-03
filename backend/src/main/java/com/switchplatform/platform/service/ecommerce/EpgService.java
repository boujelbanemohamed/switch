package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.repository.ecommerce.EpgMerchantConfigRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import com.switchplatform.platform.service.authorization.AuthorizationEngine;
import com.switchplatform.platform.service.issuing.CardAccountService;
import com.switchplatform.platform.service.issuing.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpgService {

    private final EpgTransactionRepository epgTransactionRepository;
    private final EpgMerchantConfigRepository epgMerchantConfigRepository;

    private final AuthorizationEngine authorizationEngine;
    private final CardService cardService;
    private final CardAccountService cardAccountService;

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
                .build();

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
    public EpgTransaction authorizeTransaction(UUID txnId, UUID cardId, String cavv, String eci) {
        EpgTransaction txn = getOrThrow(txnId);
        if (txn.getStatus() != EpgTransaction.Status.INITIATED
                && txn.getStatus() != EpgTransaction.Status.AUTHENTICATED) {
            throw new IllegalStateException("Transaction must be INITIATED or AUTHENTICATED before authorize: " + txnId);
        }

        Card card = cardService.getCard(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        CardAccount account = cardAccountService.getAccount(card.getCardAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Card account not found: " + card.getCardAccountId()));

        AuthorizationEngine.AuthorizationRequest request = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(cardId)
                .cardholderId(card.getCardholderId())
                .merchantId(txn.getMerchantId().toString())
                .amount(txn.getAmount())
                .currencyCode(txn.getCurrencyCode())
                .mti("0100")
                .stan(txn.getMerchantTransactionId())
                .panHash(txn.getPanHash())
                .cardType(card.getCardType() != null ? card.getCardType().name() : null)
                .cardBrand(card.getCardBrand() != null ? card.getCardBrand().name() : null)
                .build();

        AuthorizationEngine.AuthorizationResponse response = authorizationEngine.authorize(request);

        if (response.getDecision() == AuthorizationEngine.AuthorizationResponse.Decision.APPROVED) {
            txn.setStatus(EpgTransaction.Status.AUTHORIZED);
            txn.setCavv(cavv);
            txn.setEci(eci);
            txn.setAuthorizedAt(OffsetDateTime.now());
            txn.setUpdatedAt(OffsetDateTime.now());
            log.info("EPG transaction authorized: id={}, authDecisionId={}",
                    txnId, response.getAuthDecisionId());
        } else {
            txn.setStatus(EpgTransaction.Status.FAILED);
            txn.setErrorCode(response.getResponseCode());
            txn.setErrorDescription(response.getReason());
            txn.setUpdatedAt(OffsetDateTime.now());
            log.warn("EPG transaction declined: id={}, reason={}, code={}",
                    txnId, response.getReason(), response.getResponseCode());
        }

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
        log.info("EPG transaction captured: id={}, amount={}", txnId, txn.getAmount());
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
    public List<EpgMerchantConfig> getAllMerchantConfigs() {
        return epgMerchantConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public EpgMerchantConfig getMerchantConfig(UUID merchantId) {
        return epgMerchantConfigRepository.findByMerchantId(merchantId).orElse(null);
    }

    @Transactional
    public EpgMerchantConfig createMerchantConfig(UUID merchantId, String apiKeyHash, String apiSecretHash, String webhookUrl) {
        EpgMerchantConfig config = EpgMerchantConfig.builder()
                .merchantId(merchantId)
                .apiKeyHash(apiKeyHash)
                .apiSecretHash(apiSecretHash)
                .webhookUrl(webhookUrl)
                .isActive(true)
                .build();

        epgMerchantConfigRepository.save(config);
        log.info("EPG merchant config created: merchantId={}, id={}", merchantId, config.getId());
        return config;
    }

    @Transactional
    public EpgMerchantConfig updateMerchantConfig(UUID id, UUID merchantId, String apiKeyHash, String apiSecretHash, String webhookUrl, Boolean isActive) {
        EpgMerchantConfig config = epgMerchantConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant config not found: " + id));

        if (merchantId != null) config.setMerchantId(merchantId);
        if (apiKeyHash != null) config.setApiKeyHash(apiKeyHash);
        if (apiSecretHash != null) config.setApiSecretHash(apiSecretHash);
        if (webhookUrl != null) config.setWebhookUrl(webhookUrl);
        if (isActive != null) config.setIsActive(isActive);

        config.setUpdatedAt(OffsetDateTime.now());
        epgMerchantConfigRepository.save(config);
        log.info("EPG merchant config updated: id={}", id);
        return config;
    }

    @Transactional
    public void deleteMerchantConfig(UUID id) {
        if (!epgMerchantConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("Merchant config not found: " + id);
        }
        epgMerchantConfigRepository.deleteById(id);
        log.info("EPG merchant config deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<EpgTransaction> getAllTransactions() {
        return epgTransactionRepository.findAll();
    }

    private EpgTransaction getOrThrow(UUID txnId) {
        return epgTransactionRepository.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txnId));
    }
}
