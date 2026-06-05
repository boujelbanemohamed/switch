package com.switchplatform.platform.model.credit;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "credit_line_id", nullable = false)
    private UUID creditLineId;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "opening_balance", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "purchases_total", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal purchasesTotal = BigDecimal.ZERO;

    @Column(name = "payments_total", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal paymentsTotal = BigDecimal.ZERO;

    @Column(name = "interest_charged", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal interestCharged = BigDecimal.ZERO;

    @Column(name = "fees_charged", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal feesCharged = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "minimum_payment", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal minimumPayment = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_in_full", nullable = false)
    @Builder.Default
    private Boolean paidInFull = false;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatementStatus status = StatementStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum StatementStatus {
        OPEN, PAID, OVERDUE
    }
}
