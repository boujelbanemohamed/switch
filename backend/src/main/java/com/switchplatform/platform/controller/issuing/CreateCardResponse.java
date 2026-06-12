package com.switchplatform.platform.controller.issuing;

import com.switchplatform.platform.model.issuing.Card;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCardResponse(
        UUID id,
        UUID cardholderId,
        UUID cardAccountId,
        String cardNumber,
        String cvv,
        LocalDate expiryDate,
        String cardNumberSuffix,
        Card.CardStatus status,
        String cardType,
        String cardBrand
) {
    public static CreateCardResponse from(Card card, String rawCardNumber, String rawCvv) {
        return new CreateCardResponse(
                card.getId(),
                card.getCardholderId(),
                card.getCardAccountId(),
                rawCardNumber,
                rawCvv,
                card.getExpiryDate(),
                card.getCardNumberSuffix(),
                card.getStatus(),
                card.getCardType() != null ? card.getCardType().name() : null,
                card.getCardBrand() != null ? card.getCardBrand().name() : null
        );
    }
}
