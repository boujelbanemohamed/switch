package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
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
public class AcsService {

    private final Map<UUID, AcsAuthentication> authentications = new ConcurrentHashMap<>();
    private final Map<UUID, AcsChallenge> challenges = new ConcurrentHashMap<>();

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

        authentications.put(auth.getId(), auth);
        log.info("ACS authentication created: id={}, txn={}", auth.getId(), transactionId);
        return auth;
    }

    public AcsAuthentication getAuthentication(UUID authId) {
        return authentications.get(authId);
    }

    public List<AcsAuthentication> getAuthenticationsByCard(UUID cardId) {
        return authentications.values().stream()
                .filter(a -> cardId.equals(a.getCardId()))
                .sorted(Comparator.comparing(AcsAuthentication::getCreatedAt).reversed())
                .toList();
    }

    public AcsAuthentication requestChallenge(UUID authId) {
        AcsAuthentication auth = authentications.get(authId);
        if (auth == null) {
            throw new IllegalArgumentException("Authentication not found: " + authId);
        }
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    public AcsAuthentication authenticate(UUID authId, String authValue, String eci) {
        AcsAuthentication auth = authentications.get(authId);
        if (auth == null) {
            throw new IllegalArgumentException("Authentication not found: " + authId);
        }
        auth.setStatus(AcsAuthentication.Status.AUTHENTICATED);
        auth.setAuthenticationValue(authValue);
        auth.setEci(eci);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    public AcsAuthentication failAuthentication(UUID authId) {
        AcsAuthentication auth = authentications.get(authId);
        if (auth == null) {
            throw new IllegalArgumentException("Authentication not found: " + authId);
        }
        auth.setStatus(AcsAuthentication.Status.FAILED);
        auth.setUpdatedAt(OffsetDateTime.now());
        return auth;
    }

    public AcsChallenge createChallenge(UUID authId, AcsChallenge.ChallengeType type) {
        AcsAuthentication auth = authentications.get(authId);
        if (auth == null) {
            throw new IllegalArgumentException("Authentication not found: " + authId);
        }

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

        challenges.put(challenge.getId(), challenge);
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setUpdatedAt(OffsetDateTime.now());
        log.info("ACS challenge created: id={}, type={}, auth={}", challenge.getId(), type, authId);
        return challenge;
    }

    public AcsChallenge verifyChallenge(UUID challengeId) {
        AcsChallenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            throw new IllegalArgumentException("Challenge not found: " + challengeId);
        }
        challenge.setStatus(AcsChallenge.Status.VERIFIED);
        challenge.setVerifiedAt(OffsetDateTime.now());
        challenge.setUpdatedAt(OffsetDateTime.now());

        AcsAuthentication auth = authentications.get(challenge.getAuthenticationId());
        if (auth != null) {
            auth.setStatus(AcsAuthentication.Status.AUTHENTICATED);
            auth.setUpdatedAt(OffsetDateTime.now());
        }
        return challenge;
    }

    public AcsChallenge getChallenge(UUID challengeId) {
        return challenges.get(challengeId);
    }

    public List<AcsChallenge> getChallengesByAuth(UUID authId) {
        return challenges.values().stream()
                .filter(c -> authId.equals(c.getAuthenticationId()))
                .sorted(Comparator.comparing(AcsChallenge::getCreatedAt).reversed())
                .toList();
    }
}
