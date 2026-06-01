package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PinFailedEvent(
    UUID cardId,
    UUID transactionId,
    UUID pinManagementId,
    int attemptCount,
    OffsetDateTime timestamp
) {}
