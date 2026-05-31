package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import com.switchplatform.platform.repository.ecommerce.AcsAuthenticationRepository;
import com.switchplatform.platform.repository.ecommerce.AcsChallengeRepository;
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
public class AcsService {

    private final AcsAuthenticationRepository authRepository;
    private final AcsChallengeRepository challengeRepository;

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

        if (auth.getId() == null) {
            auth.setId(UUID.randomUUID());
        }

        authRepository.save(auth);
        log.info("ACS authentication created: id={}, txn={}", auth.getId(), transactionId);
        return auth;
    }

    @Transactional(readOnly = true)
    public AcsAuthentication getAuthentication(UUID authId) {
        return authRepository.findById(authId).orElse(null);
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

        if (challenge.getId() == null) {
            challenge.setId(UUID.randomUUID());
        }

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
}
