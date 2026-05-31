package com.switchplatform.platform.model.authorization;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_limits_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLimitUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "limit_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LimitType limitType;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "limit_amount", precision = 18, scale = 3)
    private BigDecimal limitAmount;

    @Column(name = "used_amount", precision = 18, scale = 3)
    private BigDecimal usedAmount;

    @Column(name = "count_used")
    private Integer countUsed = 0;

    @Column(name = "count_max")
    private Integer countMax;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "reset_at")
    private OffsetDateTime resetAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public void incrementUsage(java.math.BigDecimal amount) {
        this.usedAmount = (this.usedAmount != null ? this.usedAmount : java.math.BigDecimal.ZERO).add(amount);
        this.countUsed = (this.countUsed != null ? this.countUsed : 0) + 1;
    }

    public enum LimitType {
        DAILY, WEEKLY, MONTHLY, SINGLE, CONSECUTIVE
    }
}
