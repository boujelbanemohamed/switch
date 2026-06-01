package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeePostedEvent(
    UUID feeId,
    UUID transactionId,
    String feeType,
    String amount,
    String currency,
    String merchantId,
    OffsetDateTime timestamp
) {}
