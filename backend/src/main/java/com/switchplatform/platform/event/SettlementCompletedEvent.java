package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementCompletedEvent(
    UUID settlementId,
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    String status,
    OffsetDateTime timestamp
) {}
