package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "reward_id")
    private UUID rewardId;

    @Column(name = "points_spent", nullable = false, precision = 18, scale = 3)
    private BigDecimal pointsSpent;

    @Column(name = "balance_credit_amount", precision = 18, scale = 3)
    private BigDecimal balanceCreditAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RedemptionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum RedemptionStatus {
        PENDING, COMPLETED, CANCELLED
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = RedemptionStatus.PENDING;
    }
}
