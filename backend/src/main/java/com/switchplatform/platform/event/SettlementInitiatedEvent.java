package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementInitiatedEvent(
    UUID settlementId,
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    OffsetDateTime timestamp
) {}
