package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClearingFileGeneratedEvent(
    UUID clearingId,
    String date,
    String format,
    String participantCode,
    int size,
    OffsetDateTime timestamp
) {}
