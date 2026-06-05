package com.switchplatform.platform.model.clearing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clearing_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clearing_date", nullable = false)
    private LocalDate clearingDate;

    @Column(name = "batch_number", length = 64)
    private String batchNumber;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "acquiring_participant_id", nullable = false)
    private UUID acquiringParticipantId;

    @Column(name = "issuing_participant_id", nullable = false)
    private UUID issuingParticipantId;

    @Column(name = "pan_hash", length = 64)
    private String panHash;

    @Column(precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "interchange_amount", precision = 18, scale = 3)
    private BigDecimal interchangeAmount;

    @Column(name = "fee_amount", precision = 18, scale = 3)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 18, scale = 3)
    private BigDecimal netAmount;

    @Column(name = "message_type", length = 10)
    private String messageType;

    @Column(name = "transaction_date")
    private OffsetDateTime transactionDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "interchange_fee", precision = 18, scale = 3)
    private BigDecimal interchangeFee;

    @Column(name = "interchange_breakdown", columnDefinition = "TEXT")
    private String interchangeBreakdown;

    @Column(name = "merchant_number", length = 10)
    private String merchantNumber;

    @Column(name = "card_number", length = 19)
    private String cardNumber;

    @Column(length = 4)
    private String mcc;

    @Column(name = "authorization_number", length = 6)
    private String authorizationNumber;

    @Column(name = "origin_identifier", length = 1)
    private String originIdentifier;

    @Column(name = "operation_nature", length = 1)
    private String operationNature;

    @Column(name = "operation_code", length = 2)
    private String operationCode;

    @Column(name = "archive_reference", length = 23)
    private String archiveReference;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "trading_name", length = 255)
    private String tradingName;

    @Column(name = "slip_number", length = 6)
    private String slipNumber;

    @Column(name = "representation_flag", nullable = false)
    private boolean representationFlag = false;

    @Column(name = "dispute_id")
    private UUID disputeId;

    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum Status {
        PENDING, CLEARED, DISPUTED, REVERSED, SETTLED, FAILED
    }
}
