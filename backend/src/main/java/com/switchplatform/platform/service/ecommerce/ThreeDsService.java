package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreeDsService {

    private final Map<UUID, ThreeDsSession> sessions = new ConcurrentHashMap<>();

    public ThreeDsSession createSession(String transactionId, UUID epgTransactionId,
                                         UUID cardId, String notificationUrl) {
        ThreeDsSession session = ThreeDsSession.builder()
                .transactionId(transactionId)
                .epgTransactionId(epgTransactionId)
                .cardId(cardId)
                .threeDsVersion("2.2.0")
                .status(ThreeDsSession.Status.CREATED)
                .authenticationType("PAYMENT")
                .notificationUrl(notificationUrl)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        if (session.getId() == null) {
            session.setId(UUID.randomUUID());
        }
        if (session.getAcsReferenceNumber() == null) {
            session.setAcsReferenceNumber("ACS-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (session.getDsReferenceNumber() == null) {
            session.setDsReferenceNumber("DS-" + UUID.randomUUID().toString().substring(0, 8));
        }

        sessions.put(session.getId(), session);
        log.info("3DS session created: id={}, txn={}, version={}",
                session.getId(), transactionId, session.getThreeDsVersion());
        return session;
    }

    public ThreeDsSession getSession(UUID sessionId) {
        return sessions.get(sessionId);
    }

    public ThreeDsSession getSessionByTransactionId(String transactionId) {
        return sessions.values().stream()
                .filter(s -> transactionId.equals(s.getTransactionId()))
                .findFirst()
                .orElse(null);
    }

    public ThreeDsSession sendAuthenticationRequest(UUID sessionId, String acsUrl, String creq) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setAcsUrl(acsUrl);
        session.setCreq(creq);
        session.setStatus(ThreeDsSession.Status.AUTH_REQ_SENT);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    public ThreeDsSession receiveAuthenticationResponse(UUID sessionId, String acsTransId,
                                                          String dsTransId, String authenticationValue) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setAcsTransId(acsTransId);
        session.setDsTransId(dsTransId);
        session.setAuthenticationValue(authenticationValue);
        session.setStatus(ThreeDsSession.Status.AUTH_REQ_RECEIVED);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    public ThreeDsSession initiateChallenge(UUID sessionId, String challengeRequest, String acsUrl) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setChallengeRequest(challengeRequest);
        session.setAcsUrl(acsUrl);
        session.setStatus(ThreeDsSession.Status.CHALLENGE_SENT);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    public ThreeDsSession completeChallenge(UUID sessionId, String challengeResponse,
                                             String cres, String eci) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setChallengeResponse(challengeResponse);
        session.setCres(cres);
        session.setEci(eci);
        session.setStatus(ThreeDsSession.Status.CHALLENGE_RECEIVED);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    public ThreeDsSession completeSession(UUID sessionId, String authenticationValue) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setAuthenticationValue(authenticationValue);
        session.setStatus(ThreeDsSession.Status.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        log.info("3DS session completed: id={}", sessionId);
        return session;
    }

    public ThreeDsSession failSession(UUID sessionId, String errorDescription) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setErrorDescription(errorDescription);
        session.setStatus(ThreeDsSession.Status.ERROR);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    private ThreeDsSession getOrThrow(UUID sessionId) {
        ThreeDsSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("3DS session not found: " + sessionId);
        }
        return session;
    }
}
