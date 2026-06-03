package com.switchplatform.platform.service.fees;

import java.math.BigDecimal;
import java.util.Map;

public record FeeCalculationResult(
    BigDecimal totalFee,
    String currency,
    Map<String, Object> breakdown
) {}
