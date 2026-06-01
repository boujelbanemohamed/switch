package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClearingReceivedEvent(
    UUID clearingId,
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    String interchangeAmount,
    String feeAmount,
    OffsetDateTime timestamp
) {}
