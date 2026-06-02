package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardOperation;
import com.switchplatform.platform.repository.issuing.CardRepository;
import com.switchplatform.platform.repository.issuing.CardOperationRepository;
import com.switchplatform.platform.service.issuing.IssuingNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CardOperationRepository cardOperationRepository;
    private final IssuingNotificationService notificationService;

    @Value("${switch.pan.hash-key:}")
    private String panHashKey = "test-key-not-for-production";

    @Transactional
    public Card createCard(Card card) {
        if (card.getId() == null) {
            card.setId(UUID.randomUUID());
        }
        String cardNumber = generateCardNumber();
        card.setCardNumberHash(hashCardNumber(cardNumber));
        card.setCardNumberSuffix(cardNumber.substring(cardNumber.length() - 4));
        card.setStatus(Card.CardStatus.PENDING_ACTIVATION);
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setPinAttempts(0);
        if (card.getPinMaxAttempts() == null) {
            card.setPinMaxAttempts(3);
        }
        card.setCreatedAt(OffsetDateTime.now());
        card.setUpdatedAt(OffsetDateTime.now());

        card = cardRepository.save(card);

        recordOperation(card.getId(), "CREATE", null, card.getStatus().name(), "Card created");
        log.info("Created card {} with suffix {}", card.getId(), card.getCardNumberSuffix());
        return card;
    }

    @Transactional
    public Card activateCard(UUID cardId) {
        Card card = getCardOrThrow(cardId);
        Card.CardStatus oldStatus = card.getStatus();
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setActivationDate(LocalDate.now());
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "ACTIVATE", oldStatus.name(), card.getStatus().name(), "Card activated");
        notificationService.notifyCardActivation(card);
        cardRepository.save(card);
        log.info("Activated card {}", cardId);
        return card;
    }

    @Transactional
    public Card blockCard(UUID cardId, String reason) {
        Card card = getCardOrThrow(cardId);
        Card.CardStatus oldStatus = card.getStatus();
        card.setStatus(Card.CardStatus.BLOCKED);
        card.setStatusReason(reason);
        card.setBlockDate(LocalDate.now());
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "BLOCK", oldStatus.name(), card.getStatus().name(), reason);
        notificationService.notifyCardBlocked(card, reason);
        cardRepository.save(card);
        log.info("Blocked card {} reason: {}", cardId, reason);
        return card;
    }

    @Transactional
    public Card unblockCard(UUID cardId) {
        Card card = getCardOrThrow(cardId);
        Card.CardStatus oldStatus = card.getStatus();
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setStatusReason(null);
        card.setBlockDate(null);
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "UNBLOCK", oldStatus.name(), card.getStatus().name(), "Card unblocked");
        notificationService.notifyCardUnblocked(card);
        cardRepository.save(card);
        log.info("Unblocked card {}", cardId);
        return card;
    }

    @Transactional
    public Card reportLost(UUID cardId) {
        Card card = getCardOrThrow(cardId);
        Card.CardStatus oldStatus = card.getStatus();
        card.setStatus(Card.CardStatus.LOST);
        card.setStatusReason("REPORTED_LOST");
        card.setBlockDate(LocalDate.now());
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "REPORT_LOST", oldStatus.name(), card.getStatus().name(), "Card reported lost");
        notificationService.notifyCardLost(card);
        cardRepository.save(card);
        log.info("Card {} reported as lost", cardId);
        return card;
    }

    @Transactional
    public Card reportStolen(UUID cardId) {
        Card card = getCardOrThrow(cardId);
        Card.CardStatus oldStatus = card.getStatus();
        card.setStatus(Card.CardStatus.STOLEN);
        card.setStatusReason("REPORTED_STOLEN");
        card.setBlockDate(LocalDate.now());
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "REPORT_STOLEN", oldStatus.name(), card.getStatus().name(), "Card reported stolen");
        notificationService.notifyCardStolen(card);
        cardRepository.save(card);
        log.info("Card {} reported as stolen", cardId);
        return card;
    }

    @Transactional
    public Card renewCard(UUID cardId) {
        Card oldCard = getCardOrThrow(cardId);

        oldCard.setStatus(Card.CardStatus.RENEWED);
        oldCard.setRenewalDate(LocalDate.now());
        oldCard.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "RENEW_OLD", null, Card.CardStatus.RENEWED.name(), "Card renewed, old card closed");
        cardRepository.save(oldCard);

        Card renewed = Card.builder()
                .id(UUID.randomUUID())
                .cardholderId(oldCard.getCardholderId())
                .cardAccountId(oldCard.getCardAccountId())
                .cardType(oldCard.getCardType())
                .cardBrand(oldCard.getCardBrand())
                .cardNetwork(oldCard.getCardNetwork())
                .productCode(oldCard.getProductCode())
                .embossedLine1(oldCard.getEmbossedLine1())
                .embossedLine2(oldCard.getEmbossedLine2())
                .expiryDate(LocalDate.now().plusYears(3))
                .contactlessEnabled(oldCard.getContactlessEnabled())
                .onlineEnabled(oldCard.getOnlineEnabled())
                .internationalEnabled(oldCard.getInternationalEnabled())
                .ecommerceEnabled(oldCard.getEcommerceEnabled())
                .atmEnabled(oldCard.getAtmEnabled())
                .magStripeEnabled(oldCard.getMagStripeEnabled())
                .chipEnabled(oldCard.getChipEnabled())
                .dailyLimit(oldCard.getDailyLimit())
                .weeklyLimit(oldCard.getWeeklyLimit())
                .monthlyLimit(oldCard.getMonthlyLimit())
                .singleTxnLimit(oldCard.getSingleTxnLimit())
                .status(Card.CardStatus.PENDING_ACTIVATION)
                .pinAttempts(0)
                .isRenewable(true)
                .isReissuable(true)
                .embossed(oldCard.getEmbossed())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        String newCardNumber = generateCardNumber();
        renewed.setCardNumberHash(hashCardNumber(newCardNumber));
        renewed.setCardNumberSuffix(newCardNumber.substring(newCardNumber.length() - 4));

        renewed = cardRepository.save(renewed);
        recordOperation(renewed.getId(), "CREATE", null, renewed.getStatus().name(), "Renewed card");
        notificationService.notifyCardRenewed(oldCard, renewed);
        log.info("Renewed card {} -> new card {}", cardId, renewed.getId());
        return renewed;
    }

    @Transactional(readOnly = true)
    public Optional<Card> getCard(UUID cardId) {
        return cardRepository.findById(cardId);
    }

    @Transactional(readOnly = true)
    public Optional<Card> getCardBySuffix(String suffix) {
        return cardRepository.findByCardNumberSuffix(suffix);
    }

    @Transactional
    public Card updateCardLimits(UUID cardId, BigDecimal daily, BigDecimal weekly, BigDecimal monthly, BigDecimal single) {
        Card card = getCardOrThrow(cardId);
        card.setDailyLimit(daily);
        card.setWeeklyLimit(weekly);
        card.setMonthlyLimit(monthly);
        card.setSingleTxnLimit(single);
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "UPDATE_LIMITS", null, null,
                "Limits updated - daily: " + daily + " weekly: " + weekly +
                " monthly: " + monthly + " single: " + single);
        cardRepository.save(card);
        log.info("Updated limits for card {}", cardId);
        return card;
    }

    @Transactional
    public Card changePin(UUID cardId, String newPinBlock) {
        Card card = getCardOrThrow(cardId);
        card.setPinBlock(newPinBlock);
        card.setPinLastUpdated(OffsetDateTime.now());
        card.setPinAttempts(0);
        card.setUpdatedAt(OffsetDateTime.now());
        recordOperation(cardId, "CHANGE_PIN", null, null, "PIN changed");
        notificationService.notifyPinChanged(card);
        cardRepository.save(card);
        log.info("PIN changed for card {}", cardId);
        return card;
    }

    @Transactional
    public boolean verifyPin(UUID cardId, String pinBlock) {
        Card card = getCardOrThrow(cardId);

        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            log.warn("PIN verification failed - card {} is blocked", cardId);
            return false;
        }

        if (card.getPinAttempts() >= card.getPinMaxAttempts()) {
            card.setStatus(Card.CardStatus.BLOCKED);
            card.setStatusReason("MAX_PIN_ATTEMPTS_EXCEEDED");
            card.setBlockDate(LocalDate.now());
            card.setUpdatedAt(OffsetDateTime.now());
            recordOperation(cardId, "BLOCK", card.getStatus().name(), Card.CardStatus.BLOCKED.name(),
                    "Blocked after " + card.getPinAttempts() + " failed PIN attempts");
            notificationService.notifyCardBlocked(card, "MAX_PIN_ATTEMPTS_EXCEEDED");
            cardRepository.save(card);
            log.warn("Card {} blocked after max PIN attempts", cardId);
            return false;
        }

        boolean matches = Objects.equals(card.getPinBlock(), pinBlock);
        if (matches) {
            card.setPinAttempts(0);
            card.setUpdatedAt(OffsetDateTime.now());
            recordOperation(cardId, "PIN_VERIFIED", null, null, "PIN verified successfully");
            log.info("PIN verified for card {}", cardId);
        } else {
            card.setPinAttempts(card.getPinAttempts() + 1);
            card.setUpdatedAt(OffsetDateTime.now());
            recordOperation(cardId, "PIN_FAILED", null, null,
                    "PIN verification failed, attempt " + card.getPinAttempts());
            log.warn("PIN verification failed for card {} (attempt {}/{})",
                    cardId, card.getPinAttempts(), card.getPinMaxAttempts());

            if (card.getPinAttempts() >= card.getPinMaxAttempts()) {
                card.setStatus(Card.CardStatus.BLOCKED);
                card.setStatusReason("MAX_PIN_ATTEMPTS_EXCEEDED");
                card.setBlockDate(LocalDate.now());
                card.setUpdatedAt(OffsetDateTime.now());
                recordOperation(cardId, "BLOCK", null, Card.CardStatus.BLOCKED.name(),
                        "Blocked after max PIN attempts");
                notificationService.notifyCardBlocked(card, "MAX_PIN_ATTEMPTS_EXCEEDED");
                log.warn("Card {} blocked after max PIN attempts", cardId);
            }
        }
        cardRepository.save(card);
        return matches;
    }

    @Transactional(readOnly = true)
    public List<Card> getCardsByCardholderId(UUID cardholderId) {
        return cardRepository.findByCardholderId(cardholderId);
    }

    @Transactional(readOnly = true)
    public List<CardOperation> getOperationsForCard(UUID cardId) {
        return cardOperationRepository.findByCardIdOrderByCreatedAtDesc(cardId);
    }

    private Card getCardOrThrow(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    private void recordOperation(UUID cardId, String type, String oldStatus, String newStatus, String reason) {
        CardOperation op = CardOperation.builder()
                .cardId(cardId)
                .operationType(type)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .createdAt(OffsetDateTime.now())
                .build();
        cardOperationRepository.save(op);
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("400000");
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String hashCardNumber(String cardNumber) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(
                    panHashKey.getBytes("UTF-8"), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(cardNumber.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("PAN hash failed", e);
        }
    }
}
