package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_rewards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_cost", nullable = false, precision = 18, scale = 3)
    private BigDecimal pointsCost;

    private Integer stock;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RewardStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum RewardStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = RewardStatus.ACTIVE;
    }
}
