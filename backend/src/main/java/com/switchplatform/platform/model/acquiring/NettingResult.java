package com.switchplatform.platform.model.acquiring;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NettingResult(
    String merchantId,
    LocalDate date,
    BigDecimal grossAmount,
    BigDecimal totalFees,
    BigDecimal netAmount,
    int transactionCount,
    String currencyCode
) {}
