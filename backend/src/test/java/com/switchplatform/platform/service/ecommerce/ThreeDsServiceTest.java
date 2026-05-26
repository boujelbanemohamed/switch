package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ThreeDsServiceTest {

    private ThreeDsService threeDsService;

    @BeforeEach
    void setUp() {
        threeDsService = new ThreeDsService();
    }

    @Test
    void shouldCreateSession() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN001", UUID.randomUUID(), UUID.randomUUID(),
                "https://merchant.com/notify");

        assertNotNull(session.getId());
        assertEquals("TXN001", session.getTransactionId());
        assertEquals(ThreeDsSession.Status.CREATED, session.getStatus());
        assertEquals("2.2.0", session.getThreeDsVersion());
        assertEquals("PAYMENT", session.getAuthenticationType());
        assertNotNull(session.getAcsReferenceNumber());
        assertNotNull(session.getDsReferenceNumber());
        assertNotNull(session.getCreatedAt());
    }

    @Test
    void shouldSendAuthenticationRequest() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN002", null, UUID.randomUUID(), null);

        ThreeDsSession updated = threeDsService.sendAuthenticationRequest(
                session.getId(), "https://acs.test/auth", "encodedCreq===");

        assertEquals(ThreeDsSession.Status.AUTH_REQ_SENT, updated.getStatus());
        assertEquals("https://acs.test/auth", updated.getAcsUrl());
        assertEquals("encodedCreq===", updated.getCreq());
    }

    @Test
    void shouldReceiveAuthenticationResponse() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN003", UUID.randomUUID(), UUID.randomUUID(), null);

        ThreeDsSession updated = threeDsService.receiveAuthenticationResponse(
                session.getId(), "ACS-TRANS-001", "DS-TRANS-001",
                "authValue123==");

        assertEquals(ThreeDsSession.Status.AUTH_REQ_RECEIVED, updated.getStatus());
        assertEquals("ACS-TRANS-001", updated.getAcsTransId());
        assertEquals("DS-TRANS-001", updated.getDsTransId());
        assertEquals("authValue123==", updated.getAuthenticationValue());
    }

    @Test
    void shouldInitiateChallenge() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN004", UUID.randomUUID(), UUID.randomUUID(), null);

        ThreeDsSession challenged = threeDsService.initiateChallenge(
                session.getId(), "challengePayload==",
                "https://acs.test/challenge");

        assertEquals(ThreeDsSession.Status.CHALLENGE_SENT, challenged.getStatus());
        assertEquals("challengePayload==", challenged.getChallengeRequest());
    }

    @Test
    void shouldCompleteChallenge() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN005", UUID.randomUUID(), UUID.randomUUID(), null);

        ThreeDsSession completed = threeDsService.completeChallenge(
                session.getId(), "challengeRes==", "cresData==", "05");

        assertEquals(ThreeDsSession.Status.CHALLENGE_RECEIVED, completed.getStatus());
        assertEquals("challengeRes==", completed.getChallengeResponse());
        assertEquals("cresData==", completed.getCres());
        assertEquals("05", completed.getEci());
    }

    @Test
    void shouldCompleteSession() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN006", UUID.randomUUID(), UUID.randomUUID(), null);

        ThreeDsSession completed = threeDsService.completeSession(
                session.getId(), "finalAuthValue==");

        assertEquals(ThreeDsSession.Status.COMPLETED, completed.getStatus());
        assertEquals("finalAuthValue==", completed.getAuthenticationValue());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void shouldFailSession() {
        ThreeDsSession session = threeDsService.createSession(
                "TXN007", UUID.randomUUID(), UUID.randomUUID(), null);

        ThreeDsSession failed = threeDsService.failSession(
                session.getId(), "3DS authentication failed");

        assertEquals(ThreeDsSession.Status.ERROR, failed.getStatus());
        assertEquals("3DS authentication failed", failed.getErrorDescription());
    }

    @Test
    void shouldGetByTransactionId() {
        threeDsService.createSession("TXN008", null, null, null);

        ThreeDsSession found = threeDsService.getSessionByTransactionId("TXN008");
        assertNotNull(found);
        assertEquals("TXN008", found.getTransactionId());

        ThreeDsSession notFound = threeDsService.getSessionByTransactionId("NONEXISTENT");
        assertNull(notFound);
    }

    @Test
    void shouldThrowWhenSessionNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> threeDsService.sendAuthenticationRequest(
                        UUID.randomUUID(), "url", "creq"));
    }
}
