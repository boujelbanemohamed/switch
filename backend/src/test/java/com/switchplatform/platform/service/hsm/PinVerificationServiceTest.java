package com.switchplatform.platform.service.hsm;

import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.PinManagement;
import com.switchplatform.platform.repository.issuing.CardRepository;
import com.switchplatform.platform.repository.issuing.PinManagementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PinVerificationServiceTest {

    @Mock private PinManagementRepository pinManagementRepository;
    @Mock private CardRepository cardRepository;
    @Mock private HsmService hsmService;
    @Mock private EventPublisher eventPublisher;

    private PinVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PinVerificationService(pinManagementRepository, cardRepository, hsmService, eventPublisher);
    }

    @Test
    void verifyPin_shouldReturnFalseWhenCardNotFound() {
        when(cardRepository.findById(any())).thenReturn(Optional.empty());
        assertFalse(service.verifyPin(UUID.randomUUID(), "1234"));
    }

    @Test
    void verifyPin_shouldReturnFalseWhenNoPinRecord() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(new Card()));
        when(pinManagementRepository.findByCardId(cardId)).thenReturn(Optional.empty());
        assertFalse(service.verifyPin(cardId, "1234"));
    }

    @Test
    void verifyPin_shouldReturnFalseWhenAttemptsExhausted() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setStatus(Card.CardStatus.ACTIVE);

        PinManagement pm = PinManagement.builder()
                .cardId(cardId)
                .pinAttempts(3)
                .maxAttempts(3)
                .pinBlock("test")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(pinManagementRepository.findByCardId(cardId)).thenReturn(Optional.of(pm));

        assertFalse(service.verifyPin(cardId, "1234"));
        assertEquals(Card.CardStatus.BLOCKED, card.getStatus());
    }

    @Test
    void verifyPin_shouldReturnTrueWhenPinMatches() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setStatus(Card.CardStatus.ACTIVE);

        PinManagement pm = PinManagement.builder()
                .cardId(cardId)
                .pinAttempts(0)
                .maxAttempts(3)
                .pan("1234567890123456")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(pinManagementRepository.findByCardId(cardId)).thenReturn(Optional.of(pm));
        when(hsmService.generatePinBlock("1234567890123456", "1234")).thenReturn("testPinBlock");
        when(hsmService.verifyPinBlock("1234567890123456", "testPinBlock", "ISO0")).thenReturn(true);

        assertTrue(service.verifyPin(cardId, "1234"));
    }

    @Test
    void setPin_shouldSaveAndReturnPinManagement() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setStatus(Card.CardStatus.ACTIVE);

        when(pinManagementRepository.findByCardId(cardId)).thenReturn(Optional.empty());
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(hsmService.generatePinBlock("", "4321")).thenReturn("newPinBlock");
        when(pinManagementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PinManagement result = service.setPin(cardId, "4321");
        assertNotNull(result);
        assertEquals(cardId, result.getCardId());
    }

    @Test
    void resetRetryCounter_shouldResetPinAttempts() {
        UUID cardId = UUID.randomUUID();
        PinManagement pm = PinManagement.builder()
                .cardId(cardId)
                .pinAttempts(2)
                .build();

        when(pinManagementRepository.findByCardId(cardId)).thenReturn(Optional.of(pm));
        service.resetRetryCounter(cardId);
        assertEquals(0, pm.getPinAttempts());
    }
}
