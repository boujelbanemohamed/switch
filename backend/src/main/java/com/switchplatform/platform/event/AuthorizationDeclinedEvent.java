package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthorizationDeclinedEvent(
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    String declineReason,
    String responseCode,
    OffsetDateTime timestamp
) {}
