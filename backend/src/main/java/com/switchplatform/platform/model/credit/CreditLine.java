package com.switchplatform.platform.model.credit;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_account_id", nullable = false)
    private UUID cardAccountId;

    @Column(name = "credit_limit", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "current_balance", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "hold_amount", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Column(name = "available_credit", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal availableCredit = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal apr = new BigDecimal("18.00");

    @Column(name = "statement_day", nullable = false)
    @Builder.Default
    private Integer statementDay = 1;

    @Column(name = "payment_due_days", nullable = false)
    @Builder.Default
    private Integer paymentDueDays = 20;

    @Column(name = "min_payment_pct", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal minPaymentPct = new BigDecimal("5.00");

    @Column(name = "min_payment_floor", precision = 18, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal minPaymentFloor = new BigDecimal("10.000");

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CreditLineStatus status = CreditLineStatus.ACTIVE;

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

    public enum CreditLineStatus {
        ACTIVE, INACTIVE, BLOCKED, CLOSED
    }
}
