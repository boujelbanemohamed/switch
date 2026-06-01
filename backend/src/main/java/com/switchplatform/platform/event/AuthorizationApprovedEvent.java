package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthorizationApprovedEvent(
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    String terminalId,
    String mcc,
    String authCode,
    UUID holdId,
    OffsetDateTime timestamp
) {}
