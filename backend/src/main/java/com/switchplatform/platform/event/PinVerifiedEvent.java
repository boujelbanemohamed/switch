package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PinVerifiedEvent(
    UUID cardId,
    UUID transactionId,
    UUID pinManagementId,
    OffsetDateTime timestamp
) {}
