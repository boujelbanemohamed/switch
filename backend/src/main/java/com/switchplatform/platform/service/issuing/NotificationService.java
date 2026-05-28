package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.model.issuing.CardAccount;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationService {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void notifyCardActivation(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_ACTIVATION")
                .message("Card " + card.getCardNumberSuffix() + " has been activated")
                .createdAt(OffsetDateTime.now()).build());
        log.info("Notification: CARD_ACTIVATION for card {}", card.getId());
    }

    public void notifyCardBlocked(Card card, String reason) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_BLOCKED")
                .message("Card " + card.getCardNumberSuffix() + " has been blocked: " + reason)
                .createdAt(OffsetDateTime.now()).build());
        log.info("Notification: CARD_BLOCKED for card {} reason: {}", card.getId(), reason);
    }

    public void notifyCardUnblocked(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_UNBLOCKED")
                .message("Card " + card.getCardNumberSuffix() + " has been unblocked")
                .createdAt(OffsetDateTime.now()).build());
    }

    public void notifyCardLost(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_LOST")
                .message("Card " + card.getCardNumberSuffix() + " reported as lost")
                .actionRequired(true).createdAt(OffsetDateTime.now()).build());
    }

    public void notifyCardStolen(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_STOLEN")
                .message("Card " + card.getCardNumberSuffix() + " reported as stolen")
                .actionRequired(true).createdAt(OffsetDateTime.now()).build());
    }

    public void notifyCardRenewed(Card oldCard, Card newCard) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(oldCard.getId()).cardholderId(oldCard.getCardholderId())
                .type("CARD_RENEWED")
                .message("Card " + oldCard.getCardNumberSuffix() + " renewed to " + newCard.getCardNumberSuffix())
                .createdAt(OffsetDateTime.now()).build());
    }

    public void notifyCardExpiring(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("CARD_EXPIRING")
                .message("Card " + card.getCardNumberSuffix() + " is expiring soon")
                .actionRequired(true).createdAt(OffsetDateTime.now()).build());
    }

    public void notifyPinChanged(Card card) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(card.getId()).cardholderId(card.getCardholderId())
                .type("PIN_CHANGED")
                .message("PIN changed for card " + card.getCardNumberSuffix())
                .createdAt(OffsetDateTime.now()).build());
    }

    public void notifyLowBalance(CardAccount account) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(null).cardholderId(account.getCardholderId())
                .type("LOW_BALANCE")
                .message("Low balance: " + account.getAvailableBalance() + " " + account.getCurrencyCode())
                .actionRequired(true).createdAt(OffsetDateTime.now()).build());
    }

    public void notifyTransactionDeclined(UUID cardId, UUID cardholderId, BigDecimal amount, String reason) {
        notifications.add(Notification.builder()
                .id(UUID.randomUUID()).cardId(cardId).cardholderId(cardholderId)
                .type("TRANSACTION_DECLINED")
                .message("Transaction of " + amount + " declined: " + reason)
                .createdAt(OffsetDateTime.now()).build());
    }

    public List<Notification> getNotificationsByCardholder(UUID cardholderId) {
        return notifications.stream()
                .filter(n -> cardholderId.equals(n.getCardholderId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .toList();
    }

    public List<Notification> getNotificationsByCard(UUID cardId) {
        return notifications.stream()
                .filter(n -> cardId.equals(n.getCardId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .toList();
    }

    public List<Notification> listAll() {
        return List.copyOf(notifications);
    }

    @Data
    @Builder
    public static class Notification {
        private UUID id;
        private UUID cardId;
        private UUID cardholderId;
        private String type;
        private String message;
        @Builder.Default
        private boolean actionRequired = false;
        private OffsetDateTime createdAt;
    }
}
