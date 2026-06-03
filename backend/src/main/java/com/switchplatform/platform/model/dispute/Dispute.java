package com.switchplatform.platform.model.dispute;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "disputes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {

    public enum DisputeType {
        FRAUD, NOT_RECEIVED, DUPLICATE, INCORRECT_AMOUNT,
        QUALITY_ISSUE, CANCELLED, CREDIT_NOT_PROCESSED, OTHER
    }

    public enum DisputeStatus {
        OPEN, UNDER_REVIEW, EVIDENCE_REQUESTED, EVIDENCE_SUBMITTED,
        REPRESENTMENT, PRE_ARBITRATION, ARBITRATION, WON, LOST, WITHDRAWN
    }

    public enum InitiatedBy {
        CARDHOLDER, MERCHANT, ISSUER, ACQUIRER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dispute_number", nullable = false, unique = true, length = 32)
    private String disputeNumber;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "clearing_record_id")
    private UUID clearingRecordId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "acquiring_participant_id")
    private UUID acquiringParticipantId;

    @Column(name = "issuing_participant_id")
    private UUID issuingParticipantId;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 30)
    private DisputeType disputeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "reason_code", length = 10)
    private String reasonCode;

    @Column(name = "reason_description", columnDefinition = "TEXT")
    private String reasonDescription;

    @Column(name = "evidence_deadline")
    private OffsetDateTime evidenceDeadline;

    @Column(name = "resolution_deadline")
    private OffsetDateTime resolutionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false, length = 20)
    private InitiatedBy initiatedBy;

    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
