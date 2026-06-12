package com.switchplatform.platform.service.hsm;

import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.PinManagement;
import com.switchplatform.platform.repository.issuing.CardRepository;
import com.switchplatform.platform.repository.issuing.PinManagementRepository;
import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.event.PinVerifiedEvent;
import com.switchplatform.platform.event.PinFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinVerificationService {

    private static final int MAX_RETRIES = 3;

    private final PinManagementRepository pinManagementRepository;
    private final CardRepository cardRepository;
    private final HsmService hsmService;
    private final EventPublisher eventPublisher;

    @Transactional
    public boolean verifyPin(UUID cardId, String pin) {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            log.warn("Card not found: {}", cardId);
            return false;
        }
        Card card = cardOpt.get();

        PinManagement pinMgmt = pinManagementRepository.findByCardId(cardId)
                .orElse(null);
        if (pinMgmt == null) {
            log.warn("No PIN record for card: {}", cardId);
            return false;
        }

        int attempts = pinMgmt.getPinAttempts() != null ? pinMgmt.getPinAttempts() : 0;
        int max = pinMgmt.getMaxAttempts() != null ? pinMgmt.getMaxAttempts() : MAX_RETRIES;
        if (attempts >= max) {
            log.warn("Card {} PIN retry exhausted, blocking card", cardId);
            card.setStatus(Card.CardStatus.BLOCKED);
            cardRepository.save(card);
            return false;
        }

        String pan = pinMgmt.getPan() != null ? pinMgmt.getPan() : "";
        String pinBlock = hsmService.generatePinBlock(pan, pin);
        boolean verified = hsmService.verifyPinBlock(pan, pinBlock, "ISO0");

        if (verified) {
            pinMgmt.setPinAttempts(0);
            pinMgmt.setLastChanged(OffsetDateTime.now());
            pinManagementRepository.save(pinMgmt);
            log.info("PIN verified for card {}", cardId);

            eventPublisher.publishPinVerified(new PinVerifiedEvent(
                    cardId, null, pinMgmt.getId(), OffsetDateTime.now()));

            return true;
        } else {
            pinMgmt.setPinAttempts(attempts + 1);
            pinMgmt.setLastAttempt(OffsetDateTime.now());
            pinManagementRepository.save(pinMgmt);
            log.warn("PIN verification failed for card {}, attempt {}/{}",
                    cardId, attempts + 1, max);

            eventPublisher.publishPinFailed(new PinFailedEvent(
                    cardId, null, pinMgmt.getId(), attempts + 1, OffsetDateTime.now()));

            if (attempts + 1 >= max) {
                card.setStatus(Card.CardStatus.BLOCKED);
                cardRepository.save(card);
            }
            return false;
        }
    }

    @Transactional
    public PinManagement setPin(UUID cardId, String pin) {
        PinManagement mgmt = pinManagementRepository.findByCardId(cardId)
                .orElse(PinManagement.builder()
                        .cardId(cardId)
                        .build());

        String pan = mgmt.getPan() != null ? mgmt.getPan() : "";
        String pinBlock = hsmService.generatePinBlock(pan, pin);
        mgmt.setPinBlock(pinBlock);
        mgmt.setPinHash(hashPin(pin));
        mgmt.setPinFormat("ISO0");
        mgmt.setPinAttempts(0);
        mgmt.setLastChanged(OffsetDateTime.now());

        PinManagement saved = pinManagementRepository.save(mgmt);

        log.info("PIN set for card {}", cardId);
        return saved;
    }

    @Transactional
    public void resetRetryCounter(UUID cardId) {
        pinManagementRepository.findByCardId(cardId).ifPresent(pm -> {
            pm.setPinAttempts(0);
            pinManagementRepository.save(pm);
            log.info("PIN retry counter reset for card {}", cardId);
        });
    }

    @Transactional
    public void blockCard(UUID cardId) {
        cardRepository.findById(cardId).ifPresent(card -> {
            card.setStatus(Card.CardStatus.BLOCKED);
            cardRepository.save(card);
            log.info("Card {} blocked due to PIN retries exhausted", cardId);
        });
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("PIN hashing failed", e);
        }
    }
}
