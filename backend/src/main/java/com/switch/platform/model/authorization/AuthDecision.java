package com.switch.platform.model.authorization;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "cardholder_id")
    private UUID cardholderId;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "terminal_id", length = 8)
    private String terminalId;

    @Column(precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(length = 4)
    private String mti;

    @Column(length = 12)
    private String stan;

    @Column(length = 12)
    private String rrn;

    @Column(name = "pan_hash", length = 128)
    private String panHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Decision decision;

    @Column(name = "response_code", length = 2)
    private String responseCode;

    @Column(name = "response_reason", length = 255)
    private String responseReason;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "rule_name", length = 255)
    private String ruleName;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "card_balance_before", precision = 18, scale = 3)
    private BigDecimal cardBalanceBefore;

    @Column(name = "card_balance_after", precision = 18, scale = 3)
    private BigDecimal cardBalanceAfter;

    @Column(name = "fraud_score")
    private Integer fraudScore;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "velocity_used")
    private Integer velocityUsed;

    @Column(name = "velocity_max")
    private Integer velocityMax;

    @Column(name = "limit_used", precision = 18, scale = 3)
    private BigDecimal limitUsed;

    @Column(name = "limit_max", precision = 18, scale = 3)
    private BigDecimal limitMax;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (requestedAt == null) requestedAt = OffsetDateTime.now();
    }

    public enum Decision {
        APPROVED, DECLINED, CHALLENGED, REVIEW, TIMEOUT, ERROR
    }
}
