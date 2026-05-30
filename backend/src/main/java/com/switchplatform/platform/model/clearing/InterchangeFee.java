package com.switchplatform.platform.model.clearing;

import java.math.BigDecimal;

public record InterchangeFee(
    String brand,
    String cardType,
    String region,
    String mcc,
    BigDecimal flatFee,
    BigDecimal percentageFee
) {}
