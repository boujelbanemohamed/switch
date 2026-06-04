package com.switchplatform.platform.model.standin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stand_in_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandInRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issuer_participant_id")
    private UUID issuerParticipantId;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "max_amount", nullable = false, precision = 18, scale = 3)
    private BigDecimal maxAmount;

    @Column(name = "daily_count_limit", nullable = false)
    private Integer dailyCountLimit;

    @Column(name = "daily_amount_limit", nullable = false, precision = 18, scale = 3)
    private BigDecimal dailyAmountLimit;

    @Column(name = "allowed_mcc", columnDefinition = "TEXT")
    private String allowedMcc;

    @Column(name = "decline_if_no_rule", nullable = false)
    private Boolean declineIfNoRule;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        setDefaults();
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        setDefaults();
    }

    private void setDefaults() {
        if (enabled == null) enabled = true;
        if (declineIfNoRule == null) declineIfNoRule = true;
        if (maxAmount == null) maxAmount = BigDecimal.ZERO;
        if (dailyCountLimit == null) dailyCountLimit = 5;
        if (dailyAmountLimit == null) dailyAmountLimit = BigDecimal.ZERO;
        if (cardBrand == null) cardBrand = "ALL";
        if (allowedMcc == null) allowedMcc = "*";
    }
}
