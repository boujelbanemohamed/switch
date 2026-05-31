package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import com.switchplatform.platform.repository.ecommerce.AcsAuthenticationRepository;
import com.switchplatform.platform.repository.ecommerce.AcsChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcsServiceTest {

    private AcsService acsService;
    private final ConcurrentHashMap<UUID, AcsAuthentication> authStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AcsChallenge> challengeStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        authStore.clear();
        challengeStore.clear();

        AcsAuthenticationRepository authRepository = mock(AcsAuthenticationRepository.class);
        AcsChallengeRepository challengeRepository = mock(AcsChallengeRepository.class);

        when(authRepository.save(any())).thenAnswer(inv -> {
            AcsAuthentication a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            authStore.put(a.getId(), a);
            return a;
        });
        when(authRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(authStore.get(inv.getArgument(0))));
        when(authRepository.findByCardId(any())).thenAnswer(inv -> {
            UUID cardId = inv.getArgument(0);
            return authStore.values().stream()
                    .filter(a -> cardId.equals(a.getCardId()))
                    .sorted(Comparator.comparing(AcsAuthentication::getCreatedAt).reversed())
                    .toList();
        });

        when(challengeRepository.save(any())).thenAnswer(inv -> {
            AcsChallenge c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            challengeStore.put(c.getId(), c);
            return c;
        });
        when(challengeRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(challengeStore.get(inv.getArgument(0))));
        when(challengeRepository.findByAuthenticationId(any())).thenAnswer(inv -> {
            UUID authId = inv.getArgument(0);
            return challengeStore.values().stream()
                    .filter(c -> authId.equals(c.getAuthenticationId()))
                    .sorted(Comparator.comparing(AcsChallenge::getCreatedAt).reversed())
                    .toList();
        });

        acsService = new AcsService(authRepository, challengeRepository);
    }

    @Test
    void shouldCreateAuthentication() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN001", UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(150.00), "USD");

        assertNotNull(auth.getId());
        assertEquals("TXN001", auth.getTransactionId());
        assertEquals(AcsAuthentication.Status.CREATED, auth.getStatus());
        assertEquals("2.2.0", auth.getThreeDsVersion());
        assertNotNull(auth.getCreatedAt());
    }

    @Test
    void shouldRequestChallenge() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN002", UUID.randomUUID(), null,
                BigDecimal.valueOf(100), "EUR");

        AcsAuthentication challenged = acsService.requestChallenge(auth.getId());

        assertEquals(AcsAuthentication.Status.CHALLENGE_REQUIRED, challenged.getStatus());
    }

    @Test
    void shouldAuthenticate() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN003", UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(200), "TND");

        AcsAuthentication authenticated = acsService.authenticate(
                auth.getId(), "AAABBCCDD==", "05");

        assertEquals(AcsAuthentication.Status.AUTHENTICATED, authenticated.getStatus());
        assertEquals("AAABBCCDD==", authenticated.getAuthenticationValue());
        assertEquals("05", authenticated.getEci());
    }

    @Test
    void shouldFailAuthentication() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN004", UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(500), "USD");

        AcsAuthentication failed = acsService.failAuthentication(auth.getId());

        assertEquals(AcsAuthentication.Status.FAILED, failed.getStatus());
    }

    @Test
    void shouldCreateChallenge() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN005", UUID.randomUUID(), null,
                BigDecimal.valueOf(300), "USD");

        AcsChallenge challenge = acsService.createChallenge(
                auth.getId(), AcsChallenge.ChallengeType.OTP);

        assertNotNull(challenge.getId());
        assertEquals(auth.getId(), challenge.getAuthenticationId());
        assertEquals(AcsChallenge.ChallengeType.OTP, challenge.getChallengeType());
        assertEquals(AcsChallenge.Status.PENDING, challenge.getStatus());
        assertEquals(0, challenge.getAttempts());
        assertEquals(3, challenge.getMaxAttempts());
        assertNotNull(challenge.getExpiresAt());

        AcsAuthentication updated = acsService.getAuthentication(auth.getId());
        assertEquals(AcsAuthentication.Status.CHALLENGE_REQUIRED, updated.getStatus());
    }

    @Test
    void shouldVerifyChallenge() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN006", UUID.randomUUID(), null,
                BigDecimal.valueOf(100), "USD");
        AcsChallenge challenge = acsService.createChallenge(
                auth.getId(), AcsChallenge.ChallengeType.SMS);

        AcsChallenge verified = acsService.verifyChallenge(challenge.getId());

        assertEquals(AcsChallenge.Status.VERIFIED, verified.getStatus());
        assertNotNull(verified.getVerifiedAt());

        AcsAuthentication updated = acsService.getAuthentication(auth.getId());
        assertEquals(AcsAuthentication.Status.AUTHENTICATED, updated.getStatus());
    }

    @Test
    void shouldGetAuthenticationsByCard() {
        UUID cardId = UUID.randomUUID();
        acsService.createAuthentication("T1", cardId, null, BigDecimal.valueOf(100), "USD");
        acsService.createAuthentication("T2", cardId, null, BigDecimal.valueOf(200), "USD");
        acsService.createAuthentication("T3", UUID.randomUUID(), null, BigDecimal.valueOf(300), "USD");

        List<AcsAuthentication> result = acsService.getAuthenticationsByCard(cardId);
        assertEquals(2, result.size());
    }

    @Test
    void shouldGetChallengesByAuth() {
        AcsAuthentication auth = acsService.createAuthentication(
                "TXN007", UUID.randomUUID(), null,
                BigDecimal.valueOf(100), "USD");
        acsService.createChallenge(auth.getId(), AcsChallenge.ChallengeType.OTP);
        acsService.createChallenge(auth.getId(), AcsChallenge.ChallengeType.BIOMETRIC);

        List<AcsChallenge> result = acsService.getChallengesByAuth(auth.getId());
        assertEquals(2, result.size());
    }

    @Test
    void shouldThrowWhenAuthNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> acsService.requestChallenge(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,
                () -> acsService.authenticate(UUID.randomUUID(), "val", "07"));
        assertThrows(IllegalArgumentException.class,
                () -> acsService.createChallenge(UUID.randomUUID(), AcsChallenge.ChallengeType.OTP));
    }
}
