package com.switchplatform.platform.model.fees;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeSchedule {

    public enum ScheduleType {
        INTERCHANGE, SCHEME, PROCESSING, CROSS_BORDER,
        CURRENCY_CONVERSION, ATM, FIXED, COMPOSITE
    }

    public enum Status {
        DRAFT, ACTIVE, INACTIVE, ARCHIVED
    }

    public enum AppliesTo {
        ALL, ISSUER, ACQUIRER, MERCHANT, PARTICIPANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "card_product_id")
    private UUID cardProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", length = 20)
    private AppliesTo appliesTo;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
