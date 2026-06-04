package com.switchplatform.platform.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StandInUsedEvent(
    UUID authorizationId,
    String transactionId,
    UUID issuerParticipantId,
    BigDecimal amount,
    String currencyCode,
    String decision,
    String reason,
    OffsetDateTime timestamp
) {}
