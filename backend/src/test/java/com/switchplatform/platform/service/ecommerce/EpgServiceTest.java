package com.switchplatform.platform.service.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EpgServiceTest {

    private EpgService epgService;

    @BeforeEach
    void setUp() {
        epgService = new EpgService();
    }

    @Test
    void shouldInitiateTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "MCH-TXN-001",
                BigDecimal.valueOf(250.00), "USD");

        assertNotNull(txn.getId());
        assertEquals(EpgTransaction.Status.INITIATED, txn.getStatus());
        assertEquals(0, BigDecimal.valueOf(250.00).compareTo(txn.getAmount()));
        assertNotNull(txn.getCreatedAt());
    }

    @Test
    void shouldThrowForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                epgService.initiateTransaction(UUID.randomUUID(), "T2",
                        BigDecimal.valueOf(-100), "USD"));
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
                txn.getId(), "AAV12345==", "05");

        assertEquals(EpgTransaction.Status.AUTHORIZED, authorized.getStatus());
        assertEquals("AAV12345==", authorized.getCavv());
        assertEquals("05", authorized.getEci());
        assertNotNull(authorized.getAuthorizedAt());
    }

    @Test
    void shouldCaptureTransaction() {
        EpgTransaction txn = epgService.initiateTransaction(
                UUID.randomUUID(), "CAP-001", BigDecimal.valueOf(500), "EUR");
        epgService.authorizeTransaction(txn.getId(), "AAV===", "05");

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
        epgService.authorizeTransaction(txn.getId(), "AAV===", "05");
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
