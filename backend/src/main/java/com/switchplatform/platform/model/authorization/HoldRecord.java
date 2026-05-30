package com.switchplatform.platform.model.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldRecord {
    private UUID id;
    private String transactionId;
    private String cardId;
    private String cardAccountId;
    private BigDecimal amount;
    private String currencyCode;
    private String status;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant releasedAt;
}
