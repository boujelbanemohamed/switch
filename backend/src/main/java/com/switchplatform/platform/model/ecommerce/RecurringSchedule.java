package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cof_token_id", nullable = false)
    private UUID cofTokenId;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 20)
    private String frequency;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "occurrences_processed", nullable = false)
    private Integer occurrencesProcessed;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (occurrencesProcessed == null) occurrencesProcessed = 0;
        if (status == null) status = "ACTIVE";
        if (currencyCode == null) currencyCode = "TND";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
