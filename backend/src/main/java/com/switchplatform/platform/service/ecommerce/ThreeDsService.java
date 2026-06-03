package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import com.switchplatform.platform.repository.ecommerce.ThreeDsSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreeDsService {

    private final ThreeDsSessionRepository threeDsSessionRepository;

    @Transactional(readOnly = true)
    public List<ThreeDsSession> getAllSessions() {
        return threeDsSessionRepository.findAll();
    }

    @Transactional
    public ThreeDsSession cancelSession(UUID sessionId) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setStatus(ThreeDsSession.Status.TIMEOUT);
        session.setErrorDescription("Cancelled by operator");
        session.setUpdatedAt(OffsetDateTime.now());
        log.info("3DS session cancelled: id={}", sessionId);
        return session;
    }

    @Transactional
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

        threeDsSessionRepository.save(session);
        log.info("3DS session created: id={}, txn={}, version={}",
                session.getId(), transactionId, session.getThreeDsVersion());
        return session;
    }

    @Transactional(readOnly = true)
    public ThreeDsSession getSession(UUID sessionId) {
        return threeDsSessionRepository.findById(sessionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public ThreeDsSession getSessionByTransactionId(String transactionId) {
        return threeDsSessionRepository.findByTransactionId(transactionId).orElse(null);
    }

    @Transactional
    public ThreeDsSession sendAuthenticationRequest(UUID sessionId, String acsUrl, String creq) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setAcsUrl(acsUrl);
        session.setCreq(creq);
        session.setStatus(ThreeDsSession.Status.AUTH_REQ_SENT);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    @Transactional
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

    @Transactional
    public ThreeDsSession initiateChallenge(UUID sessionId, String challengeRequest, String acsUrl) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setChallengeRequest(challengeRequest);
        session.setAcsUrl(acsUrl);
        session.setStatus(ThreeDsSession.Status.CHALLENGE_SENT);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    @Transactional
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

    @Transactional
    public ThreeDsSession completeSession(UUID sessionId, String authenticationValue) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setAuthenticationValue(authenticationValue);
        session.setStatus(ThreeDsSession.Status.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        log.info("3DS session completed: id={}", sessionId);
        return session;
    }

    @Transactional
    public ThreeDsSession failSession(UUID sessionId, String errorDescription) {
        ThreeDsSession session = getOrThrow(sessionId);
        session.setErrorDescription(errorDescription);
        session.setStatus(ThreeDsSession.Status.ERROR);
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    private ThreeDsSession getOrThrow(UUID sessionId) {
        return threeDsSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("3DS session not found: " + sessionId));
    }
}
