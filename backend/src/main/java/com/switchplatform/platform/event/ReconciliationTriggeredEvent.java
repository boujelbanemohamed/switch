package com.switchplatform.platform.event;

import java.time.OffsetDateTime;

public record ReconciliationTriggeredEvent(
    String batchId,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    int authCount,
    int clearingCount,
    int settlementCount,
    int mismatchCount,
    OffsetDateTime timestamp
) {}
