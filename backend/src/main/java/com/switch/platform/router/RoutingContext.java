package com.switch.platform.router;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingContext {
    private String pan;
    private String bin;
    private BigDecimal amount;
    private String amountStr;
    private String currencyCode;
    private String mti;
    private String messageType;
    private String merchantId;
    private String terminalId;
    private String source;
    private String protocol;
    private String countryCode;
    private String stan;
    private UUID transactionId;

    public String getAmount() {
        return amountStr != null ? amountStr :
                amount != null ? amount.toPlainString() : null;
    }
}
