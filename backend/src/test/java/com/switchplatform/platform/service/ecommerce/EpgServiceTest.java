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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EpgServiceTest {

    private EpgService epgService;
    private final ConcurrentHashMap<UUID, EpgTransaction> txnStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, EpgMerchantConfig> configStore = new ConcurrentHashMap<>();
    private UUID testCardId;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        txnStore.clear();
        configStore.clear();

        EpgTransactionRepository epgTransactionRepository = mock(EpgTransactionRepository.class);
        EpgMerchantConfigRepository epgMerchantConfigRepository = mock(EpgMerchantConfigRepository.class);

        when(epgTransactionRepository.save(any())).thenAnswer(inv -> {
            EpgTransaction t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            if (t.getCreatedAt() == null) t.setCreatedAt(OffsetDateTime.now());
            txnStore.put(t.getId(), t);
            return t;
        });
        when(epgTransactionRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(txnStore.get(inv.getArgument(0))));
        when(epgTransactionRepository.findByMerchantIdAndMerchantTransactionId(any(), any()))
                .thenAnswer(inv -> {
                    UUID merchantId = inv.getArgument(0);
                    String merchantTxnId = inv.getArgument(1);
                    return txnStore.values().stream()
                            .filter(t -> merchantId.equals(t.getMerchantId())
                                    && merchantTxnId.equals(t.getMerchantTransactionId()))
                            .findFirst();
                });
        when(epgTransactionRepository.findByMerchantId(any())).thenAnswer(inv -> {
            UUID merchantId = inv.getArgument(0);
            return txnStore.values().stream()
                    .filter(t -> merchantId.equals(t.getMerchantId()))
                    .sorted(Comparator.comparing(EpgTransaction::getCreatedAt).reversed())
                    .toList();
        });

        when(epgMerchantConfigRepository.save(any())).thenAnswer(inv -> {
            EpgMerchantConfig c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            configStore.put(c.getId(), c);
            return c;
        });
        when(epgMerchantConfigRepository.findByMerchantId(any())).thenAnswer(inv -> {
            UUID merchantId = inv.getArgument(0);
            return configStore.values().stream()
                    .filter(c -> merchantId.equals(c.getMerchantId()))
                    .findFirst();
        });

        testCardId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();
        Card card = mock(Card.class);
        when(card.getId()).thenReturn(testCardId);
        when(card.getCardAccountId()).thenReturn(testAccountId);
        when(card.getCardholderId()).thenReturn(UUID.randomUUID());
        when(card.getCardType()).thenReturn(Card.CardType.DEBIT);
        when(card.getCardBrand()).thenReturn(Card.CardBrand.VISA);

        CardAccount account = mock(CardAccount.class);
        when(account.getId()).thenReturn(testAccountId);

        CardService cardService = mock(CardService.class);
        when(cardService.getCard(testCardId)).thenReturn(Optional.of(card));

        CardAccountService cardAccountService = mock(CardAccountService.class);
        when(cardAccountService.getAccount(testAccountId)).thenReturn(Optional.of(account));

        AuthorizationEngine authorizationEngine = mock(AuthorizationEngine.class);
        when(authorizationEngine.authorize(any())).thenAnswer(inv -> {
            AuthorizationEngine.AuthorizationRequest req = inv.getArgument(0);
            return AuthorizationEngine.AuthorizationResponse.builder()
                    .decision(AuthorizationEngine.AuthorizationResponse.Decision.APPROVED)
                    .responseCode("00")
                    .reason("APPROVED")
                    .authDecisionId(1L)
                    .build();
        });

        epgService = new EpgService(epgTransactionRepository, epgMerchantConfigRepository,
                authorizationEngine, cardService, cardAccountService);
    }

    @Test
    void shouldInitiateTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "MCH-TXN-001",
                BigDecimal.valueOf(250.00), "USD");

        assertEquals(EpgTransaction.Status.INITIATED, txn.getStatus());
        assertEquals("MCH-TXN-001", txn.getMerchantTransactionId());
        assertEquals(0, BigDecimal.valueOf(250.00).compareTo(txn.getAmount()));
        assertNotNull(txn.getId());
        assertNotNull(txn.getCreatedAt());
    }

    @Test
    void shouldThrowForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                epgService.initiateTransaction(UUID.randomUUID(), "NEG-001",
                        BigDecimal.valueOf(-50), "USD"));
    }

    @Test
    void shouldThrowForZeroAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                epgService.initiateTransaction(UUID.randomUUID(), "ZERO-001",
                        BigDecimal.ZERO, "USD"));
    }

    @Test
    void shouldThrowForDuplicateMerchantTxn() {
        UUID merchantId = UUID.randomUUID();
        epgService.initiateTransaction(merchantId, "DUP-001", BigDecimal.valueOf(100), "USD");
        assertThrows(IllegalArgumentException.class, () ->
                epgService.initiateTransaction(merchantId, "DUP-001", BigDecimal.valueOf(200), "USD"));
    }

    @Test
    void shouldAuthorizeTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "AUTH-001", BigDecimal.valueOf(100), "USD");

        EpgTransaction authorized = epgService.authorizeTransaction(
                txn.getId(), testCardId, "AAV12345==", "05");

        assertEquals(EpgTransaction.Status.AUTHORIZED, authorized.getStatus());
        assertEquals("AAV12345==", authorized.getCavv());
        assertEquals("05", authorized.getEci());
        assertNotNull(authorized.getAuthorizedAt());
    }

    @Test
    void shouldCaptureTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "CAP-001", BigDecimal.valueOf(500), "EUR");
        epgService.authorizeTransaction(txn.getId(), testCardId, "AAV===", "05");

        EpgTransaction captured = epgService.captureTransaction(txn.getId());

        assertEquals(EpgTransaction.Status.CAPTURED, captured.getStatus());
        assertNotNull(captured.getCapturedAt());
    }

    @Test
    void shouldFailTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "FAIL-001", BigDecimal.valueOf(100), "USD");

        EpgTransaction failed = epgService.failTransaction(
                txn.getId(), "DECLINED", "Card declined by issuer");

        assertEquals(EpgTransaction.Status.FAILED, failed.getStatus());
        assertEquals("DECLINED", failed.getErrorCode());
    }

    @Test
    void shouldRefundTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "REF-001", BigDecimal.valueOf(300), "USD");
        epgService.authorizeTransaction(txn.getId(), testCardId, "AAV===", "05");
        epgService.captureTransaction(txn.getId());

        EpgTransaction refunded = epgService.refundTransaction(txn.getId());

        assertEquals(EpgTransaction.Status.REFUNDED, refunded.getStatus());
    }

    @Test
    void shouldThrowWhenRefundNotCaptured() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "REF-BAD", BigDecimal.valueOf(100), "USD");
        assertThrows(IllegalStateException.class,
                () -> epgService.refundTransaction(txn.getId()));
    }

    @Test
    void shouldThrowWhenCaptureNotAuthorized() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "CAP-BAD", BigDecimal.valueOf(100), "USD");
        assertThrows(IllegalStateException.class,
                () -> epgService.captureTransaction(txn.getId()));
    }

    @Test
    void shouldGetByMerchantTransaction() {
        UUID merchantId = UUID.randomUUID();
        epgService.initiateTransaction(merchantId, "M1", BigDecimal.valueOf(100), "USD");
        epgService.initiateTransaction(merchantId, "M2", BigDecimal.valueOf(200), "USD");

        List<EpgTransaction> txns = epgService.getTransactionsByMerchant(merchantId);
        assertEquals(2, txns.size());
    }

    @Test
    void shouldConfigureMerchant() {
        UUID merchantId = UUID.randomUUID();
        EpgMerchantConfig config = epgService.configureMerchant(
                merchantId, "apikey123", "secret456");

        assertNotNull(config.getId());
        assertEquals(merchantId, config.getMerchantId());
        assertTrue(config.getIsActive());
    }

    @Test
    void shouldGetMerchantConfig() {
        UUID merchantId = UUID.randomUUID();
        epgService.configureMerchant(merchantId, "key1", "secret1");

        EpgMerchantConfig config = epgService.getMerchantConfig(merchantId);
        assertNotNull(config);
        assertEquals("key1", config.getApiKeyHash());
    }
}
