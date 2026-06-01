package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HoldReleasedEvent(
    UUID holdId,
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String reason,
    OffsetDateTime timestamp
) {}
