package com.switchplatform.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "message_type", length = 10)
    private String messageType;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    @Column(length = 12)
    private String stan;

    @Column(length = 12)
    private String rrn;

    @Column(length = 64)
    private String panHash;

    @Column(precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "terminal_id", length = 8)
    private String terminalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquiring_participant_id")
    private Participant acquiringParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_participant_id")
    private Participant issuingParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_participant_id")
    private Participant sourceParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_participant_id")
    private Participant destinationParticipant;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "response_code", length = 2)
    private String responseCode;

    @Column(columnDefinition = "TEXT")
    private String originalMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String parsedMessage;

    @Column(columnDefinition = "TEXT")
    private String responseMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_rule_id")
    private RoutingRule routingRule;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    @Column(name = "stand_in_used", nullable = false)
    private Boolean standInUsed = false;

    @Column(name = "pos_entry_mode", length = 3)
    private String posEntryMode;

    @Column(name = "pos_condition_code", length = 2)
    private String posConditionCode;

    @Column(length = 4)
    private String channel;

    @Column(name = "transaction_type", length = 4)
    private String transactionType;

    @Column(name = "request_at", nullable = false)
    private OffsetDateTime requestAt;

    @Column(name = "response_at")
    private OffsetDateTime responseAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (requestAt == null) requestAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Protocol {
        ISO8583, ISO20022
    }

    public enum TransactionStatus {
        PENDING, ROUTING, PROCESSING, COMPLETED, FAILED, TIMEOUT, REJECTED
    }
}
