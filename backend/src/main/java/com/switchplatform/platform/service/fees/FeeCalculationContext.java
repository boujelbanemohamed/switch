package com.switchplatform.platform.service.fees;

import java.math.BigDecimal;

public record FeeCalculationContext(
    BigDecimal amount,
    String currency,
    String brand,
    String cardType,
    String mcc,
    String region,
    String entryMode,
    String participantId,
    String merchantId,
    BigDecimal interchangeOverride
) {}
