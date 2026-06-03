package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BatchJobCompletedEvent(
    UUID jobId,
    String jobName,
    String jobType,
    String status,
    int recordsProcessed,
    int recordsFailed,
    OffsetDateTime timestamp
) {}
