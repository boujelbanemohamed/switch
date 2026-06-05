package com.switchplatform.platform.model.credit;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "installment_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "installment_plan_id", nullable = false)
    private UUID installmentPlanId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(precision = 18, scale = 3, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(name = "statement_id")
    private UUID statementId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
