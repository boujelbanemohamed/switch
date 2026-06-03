package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisputeResolvedEvent(
    UUID disputeId,
    String disputeNumber,
    String newStatus,
    String oldStatus,
    OffsetDateTime timestamp
) {}
