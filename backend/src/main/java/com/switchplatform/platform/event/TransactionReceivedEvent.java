package com.switchplatform.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionReceivedEvent(
    UUID transactionId,
    String pan,
    String amount,
    String currency,
    String merchantId,
    String terminalId,
    String mcc,
    String responseCode,
    OffsetDateTime timestamp
) {}
