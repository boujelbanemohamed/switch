package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_tiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "min_lifetime_points", nullable = false, precision = 18, scale = 3)
    private BigDecimal minLifetimePoints;

    @Column(name = "earning_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal earningMultiplier;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String benefits;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TierStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum TierStatus {
        ACTIVE, INACTIVE
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = TierStatus.ACTIVE;
        if (earningMultiplier == null) earningMultiplier = BigDecimal.ONE;
    }
}
