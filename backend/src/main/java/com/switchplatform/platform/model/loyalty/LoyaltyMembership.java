package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_memberships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cardholder_id", "program_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "tier_id", nullable = false)
    private UUID tierId;

    @Column(name = "points_balance", nullable = false, precision = 18, scale = 3)
    private BigDecimal pointsBalance;

    @Column(name = "lifetime_points", nullable = false, precision = 18, scale = 3)
    private BigDecimal lifetimePoints;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MembershipStatus status;

    public enum MembershipStatus {
        ACTIVE, SUSPENDED, CANCELLED
    }

    @PrePersist
    void onCreate() {
        if (enrolledAt == null) enrolledAt = OffsetDateTime.now();
        if (status == null) status = MembershipStatus.ACTIVE;
        if (pointsBalance == null) pointsBalance = BigDecimal.ZERO;
        if (lifetimePoints == null) lifetimePoints = BigDecimal.ZERO;
    }
}
