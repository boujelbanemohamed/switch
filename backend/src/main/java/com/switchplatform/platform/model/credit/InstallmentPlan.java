package com.switchplatform.platform.model.credit;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "installment_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "credit_line_id", nullable = false)
    private UUID creditLineId;

    @Column(name = "original_transaction_ref", length = 64)
    private String originalTransactionRef;

    @Column(name = "total_amount", precision = 18, scale = 3, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    @Column(name = "installment_amount", precision = 18, scale = 3, nullable = false)
    private BigDecimal installmentAmount;

    @Column(name = "fee_amount", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal apr;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (remainingCount == null) remainingCount = installmentCount;
    }

    public enum InstallmentStatus {
        ACTIVE, COMPLETED, DEFAULTED, CANCELLED
    }
}
