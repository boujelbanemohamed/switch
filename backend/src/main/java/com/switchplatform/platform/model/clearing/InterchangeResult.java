package com.switchplatform.platform.model.clearing;

import java.math.BigDecimal;

public record InterchangeResult(
    BigDecimal fee,
    BigDecimal percentageAmount,
    BigDecimal totalFee,
    String breakdown
) {}
