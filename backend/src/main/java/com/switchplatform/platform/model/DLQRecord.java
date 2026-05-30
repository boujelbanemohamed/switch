package com.switchplatform.platform.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DLQRecord(
        UUID id,
        String topic,
        String payload,
        String errorMessage,
        int retryCount,
        OffsetDateTime createdAt,
        OffsetDateTime lastRetryAt
) {}
