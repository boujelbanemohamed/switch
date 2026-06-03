package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisputeOpenedEvent(
    UUID disputeId,
    String disputeNumber,
    String transactionId,
    String disputeType,
    String amount,
    String currency,
    String initiatedBy,
    OffsetDateTime timestamp
) {}
