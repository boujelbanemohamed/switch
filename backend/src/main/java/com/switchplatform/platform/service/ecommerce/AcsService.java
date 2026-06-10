package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import com.switchplatform.platform.repository.ecommerce.AcsAuthenticationRepository;
import com.switchplatform.platform.repository.ecommerce.AcsChallengeRepository;
import com.switchplatform.platform.service.fraud.FraudEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcsService {

    private final AcsAuthenticationRepository authRepository;
    private final AcsChallengeRepository challengeRepository;
    private final FraudEngine fraudEngine;

    @Value("${switch.fraud.rba.score-low-threshold:30}")
    private int scoreLowThreshold;

    @Value("${switch.fraud.rba.score-high-threshold:70}")
    private int scoreHighThreshold;

    @Transactional
    public AcsAuthentication createAuthentication(String transactionId, UUID cardId, UUID merchantId,
                                                   BigDecimal amount, String currencyCode) {
        AcsAuthentication auth = AcsAuthentication.builder()
                .transactionId(transactionId)
                .cardId(cardId)
                .merchantId(merchantId)
                .amount(amount)
                .currencyCode(currencyCode)
                .status(AcsAuthentication.Status.CREATED)
                .threeDsVersion("2.2.0")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        authRepository.save(auth);
        log.info("ACS authentication created: id={}, txn={}", auth.getId(), transactionId);
        return auth;
    }

    @Transactional(readOnly = true)
    public List<AcsAuthentication> getAllAuthentications() {
        return authRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AcsAuthentication getAuthentication(UUID id) {
        return authRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AcsAuthentication> getAuthenticationsByCard(UUID cardId) {
        return authRepository.findByCardId(cardId).stream()
                .sorted(Comparator.comparing(AcsAuthentication::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public AcsAuthentication requestChallenge(UUID authId) {
        AcsAuthentication auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication not found: " + authId));
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    @Transactional
    public AcsAuthentication authenticate(UUID authId, String authValue, String eci) {
        AcsAuthentication auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication not found: " + authId));
        auth.setStatus(AcsAuthentication.Status.AUTHENTICATED);
        auth.setAuthenticationValue(authValue);
        auth.setEci(eci);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    @Transactional
    public AcsAuthentication failAuthentication(UUID authId) {
        AcsAuthentication auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication not found: " + authId));
        auth.setStatus(AcsAuthentication.Status.FAILED);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    @Transactional
    public AcsChallenge createChallenge(UUID authId, AcsChallenge.ChallengeType type) {
        AcsAuthentication auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication not found: " + authId));

        AcsChallenge challenge = AcsChallenge.builder()
                .authenticationId(authId)
                .challengeType(type)
                .status(AcsChallenge.Status.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        challengeRepository.save(challenge);
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setUpdatedAt(OffsetDateTime.now());
        log.info("ACS challenge created: id={}, type={}, auth={}", challenge.getId(), type, authId);
        return challenge;
    }

    @Transactional
    public AcsChallenge verifyChallenge(UUID challengeId) {
        AcsChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + challengeId));
        challenge.setStatus(AcsChallenge.Status.VERIFIED);
        challenge.setVerifiedAt(OffsetDateTime.now());
        challenge.setUpdatedAt(OffsetDateTime.now());

        AcsAuthentication auth = authRepository.findById(challenge.getAuthenticationId()).orElse(null);
        if (auth != null) {
            auth.setStatus(AcsAuthentication.Status.AUTHENTICATED);
            auth.setUpdatedAt(OffsetDateTime.now());
        }
        return challenge;
    }

    @Transactional(readOnly = true)
    public AcsChallenge getChallenge(UUID challengeId) {
        return challengeRepository.findById(challengeId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AcsChallenge> getChallengesByAuth(UUID authId) {
        return challengeRepository.findByAuthenticationId(authId).stream()
                .sorted(Comparator.comparing(AcsChallenge::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public AcsAuthentication evaluateRba(UUID authId, Map<String, Object> extraContext) {
        AcsAuthentication auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authentication not found: " + authId));

        FraudEngine.EvaluationContext.EvaluationContextBuilder ctxBuilder = FraudEngine.EvaluationContext.builder()
                .cardId(auth.getCardId())
                .transactionId(auth.getTransactionId())
                .amount(auth.getAmount())
                .currencyCode(auth.getCurrencyCode())
                .merchantId(auth.getMerchantId() != null ? auth.getMerchantId().toString() : null)
                .panHash(auth.getPanHash())
                .timestamp(OffsetDateTime.now());

        if (extraContext != null) {
            if (extraContext.get("deviceId") != null) ctxBuilder.deviceId((String) extraContext.get("deviceId"));
            if (extraContext.get("ipAddress") != null) ctxBuilder.ipAddress((String) extraContext.get("ipAddress"));
            if (extraContext.get("deviceType") != null) ctxBuilder.deviceType((String) extraContext.get("deviceType"));
            if (extraContext.get("os") != null) ctxBuilder.os((String) extraContext.get("os"));
            if (extraContext.get("browser") != null) ctxBuilder.browser((String) extraContext.get("browser"));
            if (extraContext.get("userAgent") != null) ctxBuilder.userAgent((String) extraContext.get("userAgent"));
            if (extraContext.get("countryCode") != null) ctxBuilder.countryCode((String) extraContext.get("countryCode"));
            if (extraContext.get("merchantCategory") != null) ctxBuilder.merchantCategory((String) extraContext.get("merchantCategory"));
            if (extraContext.get("cardholderId") != null)
                ctxBuilder.cardholderId(UUID.fromString((String) extraContext.get("cardholderId")));
        }

        FraudEngine.FraudEvaluationResult result = fraudEngine.evaluateTransaction(ctxBuilder.build());

        String riskDecision;
        if (result.getScore() >= scoreHighThreshold) {
            riskDecision = "DECLINED";
            auth.setStatus(AcsAuthentication.Status.DECLINED);
        } else if (result.getScore() >= scoreLowThreshold) {
            riskDecision = "CHALLENGE_REQUIRED";
            auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        } else {
            riskDecision = "AUTHENTICATED";
            String cavv = "AAAB" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
            auth.setAuthenticationValue(cavv);
            auth.setEci("05");
            auth.setStatus(AcsAuthentication.Status.AUTHENTICATED);
        }

        auth.setRiskScore(result.getScore());
        auth.setRiskDecision(riskDecision);
        auth.setUpdatedAt(OffsetDateTime.now());

        log.info("RBA evaluated: authId={}, score={}, riskLevel={}, decision={}, matchedRules={}",
                authId, result.getScore(), result.getRiskLevel(), riskDecision, result.getMatchedRules());

        return auth;
    }
}
