package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HoldPlacedEvent(
    UUID holdId,
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    OffsetDateTime expiresAt,
    OffsetDateTime timestamp
) {}
