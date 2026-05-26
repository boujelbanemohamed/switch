package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.Cardholder;
import com.switchplatform.platform.model.issuing.WalletToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IssuingServiceTest {

    private CardService cardService;
    private CardholderService cardholderService;
    private WalletTokenService walletTokenService;

    @BeforeEach
    void setUp() {
        cardService = new CardService();
        cardholderService = new CardholderService(cardService);
        walletTokenService = new WalletTokenService();
    }

    @Test
    void shouldCreateCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        assertNotNull(created.getId());
        assertNotNull(created.getCardNumberHash());
        assertNotNull(created.getCardNumberSuffix());
        assertEquals(4, created.getCardNumberSuffix().length());
        assertEquals(Card.CardStatus.PENDING_ACTIVATION, created.getStatus());
        assertNotNull(created.getExpiryDate());
        assertEquals(0, created.getPinAttempts());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        Card fetched = cardService.getCard(created.getId()).orElse(null);
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    void shouldActivateCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card activated = cardService.activateCard(created.getId());

        assertEquals(Card.CardStatus.ACTIVE, activated.getStatus());
        assertNotNull(activated.getActivationDate());
    }

    @Test
    void shouldBlockCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card blocked = cardService.blockCard(created.getId(), "SUSPECTED_FRAUD");

        assertEquals(Card.CardStatus.BLOCKED, blocked.getStatus());
        assertEquals("SUSPECTED_FRAUD", blocked.getStatusReason());
        assertNotNull(blocked.getBlockDate());
    }

    @Test
    void shouldUnblockCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        cardService.blockCard(created.getId(), "TEMPORARY");
        Card unblocked = cardService.unblockCard(created.getId());

        assertEquals(Card.CardStatus.ACTIVE, unblocked.getStatus());
        assertNull(unblocked.getStatusReason());
        assertNull(unblocked.getBlockDate());
    }

    @Test
    void shouldReportLostCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card lost = cardService.reportLost(created.getId());

        assertEquals(Card.CardStatus.LOST, lost.getStatus());
        assertEquals("REPORTED_LOST", lost.getStatusReason());
        assertNotNull(lost.getBlockDate());
    }

    @Test
    void shouldReportStolenCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card stolen = cardService.reportStolen(created.getId());

        assertEquals(Card.CardStatus.STOLEN, stolen.getStatus());
        assertEquals("REPORTED_STOLEN", stolen.getStatusReason());
    }

    @Test
    void shouldRenewCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);
        cardService.activateCard(created.getId());

        Card renewed = cardService.renewCard(created.getId());

        assertNotNull(renewed.getId());
        assertNotEquals(created.getId(), renewed.getId());
        assertEquals(Card.CardStatus.PENDING_ACTIVATION, renewed.getStatus());
        assertNotNull(renewed.getExpiryDate());
        assertFalse(renewed.getExpiryDate().isBefore(created.getExpiryDate()));
        assertEquals(0, renewed.getPinAttempts());

        Card oldCard = cardService.getCard(created.getId()).orElse(null);
        assertNotNull(oldCard);
        assertEquals(Card.CardStatus.RENEWED, oldCard.getStatus());
        assertNotNull(oldCard.getRenewalDate());
    }

    @Test
    void shouldUpdateLimits() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card updated = cardService.updateCardLimits(
                created.getId(),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(500)
        );

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(updated.getDailyLimit()));
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(updated.getWeeklyLimit()));
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(updated.getMonthlyLimit()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(updated.getSingleTxnLimit()));
    }

    @Test
    void shouldVerifyPin() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);
        cardService.changePin(created.getId(), "1234");

        assertTrue(cardService.verifyPin(created.getId(), "1234"));
        assertFalse(cardService.verifyPin(created.getId(), "5678"));
    }

    @Test
    void shouldBlockCardAfterMaxPinAttempts() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);
        cardService.changePin(created.getId(), "1234");

        cardService.verifyPin(created.getId(), "WRONG");
        cardService.verifyPin(created.getId(), "WRONG");
        boolean lastAttempt = cardService.verifyPin(created.getId(), "WRONG");

        assertFalse(lastAttempt);

        Card fetched = cardService.getCard(created.getId()).orElse(null);
        assertNotNull(fetched);
        assertEquals(Card.CardStatus.BLOCKED, fetched.getStatus());
        assertEquals("MAX_PIN_ATTEMPTS_EXCEEDED", fetched.getStatusReason());
    }

    @Test
    void shouldGetCardBySuffix() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        Card found = cardService.getCardBySuffix(created.getCardNumberSuffix()).orElse(null);

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void shouldReturnEmptyForNonExistentCard() {
        Optional<Card> result = cardService.getCard(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenCardNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> cardService.activateCard(UUID.randomUUID()));
    }

    @Test
    void shouldCreateCardholder() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        Cardholder created = cardholderService.createCardholder(cardholder);

        assertNotNull(created.getId());
        assertEquals(Cardholder.CardholderStatus.ACTIVE, created.getStatus());
        assertEquals("1", created.getKycLevel());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void shouldGetCardholderByEmail() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();
        cardholderService.createCardholder(cardholder);

        Cardholder found = cardholderService.getCardholderByEmail("jane.smith@example.com").orElse(null);

        assertNotNull(found);
        assertEquals("Jane", found.getFirstName());
        assertEquals("Smith", found.getLastName());
    }

    @Test
    void shouldGetCardholderById() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .build();
        Cardholder created = cardholderService.createCardholder(cardholder);

        Cardholder found = cardholderService.getCardholder(created.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void shouldUpdateKycLevel() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("Bob")
                .lastName("Brown")
                .email("bob@example.com")
                .build();
        Cardholder created = cardholderService.createCardholder(cardholder);

        Cardholder updated = cardholderService.updateKycLevel(created.getId(), 3);

        assertEquals("3", updated.getKycLevel());
    }

    @Test
    void shouldBlockCardholderAndCards() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("Charlie")
                .lastName("Wilson")
                .email("charlie@example.com")
                .build();
        Cardholder created = cardholderService.createCardholder(cardholder);

        Card card = buildTestCard();
        card.setCardholderId(created.getId());
        Card savedCard = cardService.createCard(card);

        Cardholder blocked = cardholderService.blockCardholder(created.getId());

        assertEquals(Cardholder.CardholderStatus.BLOCKED, blocked.getStatus());

        Card fetchedCard = cardService.getCard(savedCard.getId()).orElse(null);
        assertNotNull(fetchedCard);
        assertEquals(Card.CardStatus.BLOCKED, fetchedCard.getStatus());
        assertEquals("CARDHOLDER_BLOCKED", fetchedCard.getStatusReason());
    }

    @Test
    void shouldThrowForDuplicateEmail() {
        Cardholder cardholder = Cardholder.builder()
                .firstName("One")
                .lastName("User")
                .email("duplicate@example.com")
                .build();
        cardholderService.createCardholder(cardholder);

        Cardholder duplicate = Cardholder.builder()
                .firstName("Two")
                .lastName("User")
                .email("duplicate@example.com")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> cardholderService.createCardholder(duplicate));
    }

    @Test
    void shouldTokenizeCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        WalletToken token = walletTokenService.tokenizeCard(
                created.getId(), "APPLE_PAY", "device-001");

        assertNotNull(token.getId());
        assertNotNull(token.getToken());
        assertEquals(created.getId(), token.getCardId());
        assertEquals(WalletToken.TokenStatus.ACTIVE, token.getStatus());
        assertEquals(WalletToken.WalletProvider.APPLE_PAY, token.getWalletProvider());
        assertEquals("device-001", token.getDeviceId());
        assertNotNull(token.getTokenExpiry());
    }

    @Test
    void shouldDetokenize() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        WalletToken token = walletTokenService.tokenizeCard(
                created.getId(), "GOOGLE_PAY", "device-002");

        UUID cardId = walletTokenService.detokenize(token.getToken()).orElse(null);

        assertNotNull(cardId);
        assertEquals(created.getId(), cardId);
    }

    @Test
    void shouldReturnEmptyForNonExistentToken() {
        Optional<UUID> result = walletTokenService.detokenize("nonexistent-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSuspendToken() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        WalletToken token = walletTokenService.tokenizeCard(
                created.getId(), "APPLE_PAY", "device-003");

        WalletToken suspended = walletTokenService.suspendToken(token.getToken()).orElse(null);

        assertNotNull(suspended);
        assertEquals(WalletToken.TokenStatus.SUSPENDED, suspended.getStatus());
    }

    @Test
    void shouldTerminateToken() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        WalletToken token = walletTokenService.tokenizeCard(
                created.getId(), "GOOGLE_PAY", "device-004");

        WalletToken terminated = walletTokenService.terminateToken(token.getToken()).orElse(null);

        assertNotNull(terminated);
        assertEquals(WalletToken.TokenStatus.TERMINATED, terminated.getStatus());
    }

    @Test
    void shouldGetTokensByCard() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        walletTokenService.tokenizeCard(created.getId(), "APPLE_PAY", "device-005");
        walletTokenService.tokenizeCard(created.getId(), "GOOGLE_PAY", "device-006");

        List<WalletToken> tokens = walletTokenService.getTokensByCard(created.getId());

        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().allMatch(t -> t.getCardId().equals(created.getId())));
    }

    @Test
    void shouldThrowForInvalidWalletProvider() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        assertThrows(IllegalArgumentException.class,
                () -> walletTokenService.tokenizeCard(created.getId(), "BITCOIN_PAY", "device-007"));
    }

    @Test
    void shouldGetTokenById() {
        Card card = buildTestCard();
        Card created = cardService.createCard(card);

        WalletToken token = walletTokenService.tokenizeCard(
                created.getId(), "SAMSUNG_PAY", "device-008");

        WalletToken found = walletTokenService.getToken(token.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(token.getId(), found.getId());
    }

    @Test
    void shouldReturnEmptyListForCardWithNoTokens() {
        List<WalletToken> tokens = walletTokenService.getTokensByCard(UUID.randomUUID());

        assertTrue(tokens.isEmpty());
    }

    private Card buildTestCard() {
        return Card.builder()
                .cardholderId(UUID.randomUUID())
                .cardAccountId(UUID.randomUUID())
                .cardType(Card.CardType.DEBIT)
                .cardBrand(Card.CardBrand.VISA)
                .cardNetwork(Card.CardNetwork.VISA_NET)
                .productCode("VSD001")
                .embossedLine1("JOHN DOE")
                .contactlessEnabled(true)
                .onlineEnabled(true)
                .internationalEnabled(true)
                .ecommerceEnabled(true)
                .atmEnabled(true)
                .chipEnabled(true)
                .magStripeEnabled(true)
                .build();
    }
}
