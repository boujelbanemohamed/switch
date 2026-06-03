package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BatchJobStartedEvent(
    UUID jobId,
    String jobName,
    String jobType,
    OffsetDateTime timestamp
) {}
