package com.switchplatform.platform.model.acquiring;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {
    private UUID id;
    private String merchantId;
    private LocalDate settlementDate;
    private BigDecimal totalAmount;
    private BigDecimal totalFee;
    private BigDecimal netAmount;
    private String currencyCode;
    private String status;
    private String paymentRef;
    private OffsetDateTime createdAt;
    private OffsetDateTime confirmedAt;
}
