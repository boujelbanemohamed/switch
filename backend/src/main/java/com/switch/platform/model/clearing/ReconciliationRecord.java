package com.switch.platform.model.clearing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Source source;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "total_amount", precision = 18, scale = 3)
    private BigDecimal totalAmount;

    @Column(name = "total_fees", precision = 18, scale = 3)
    private BigDecimal totalFees;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "unmatched_count")
    private Integer unmatchedCount;

    @Column(name = "discrepancy_count")
    private Integer discrepancyCount;

    @Column(nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum Source {
        SWITCH, PARTICIPANT, MERCHANT, SCHEME
    }

    public enum Status {
        PENDING, MATCHED, PARTIALLY_MATCHED, DISCREPANCY, RESOLVED
    }
}
