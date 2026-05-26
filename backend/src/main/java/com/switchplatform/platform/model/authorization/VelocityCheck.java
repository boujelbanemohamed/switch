package com.switchplatform.platform.model.authorization;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "velocity_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VelocityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "velocity_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private VelocityType velocityType;

    @Column(name = "window_start")
    private OffsetDateTime windowStart;

    @Column(name = "window_end")
    private OffsetDateTime windowEnd;

    @Column(name = "current_count")
    private Integer currentCount = 0;

    @Column(name = "current_amount", precision = 18, scale = 3)
    private BigDecimal currentAmount;

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "max_amount", precision = 18, scale = 3)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum VelocityType {
        TXNS_PER_HOUR, TXNS_PER_DAY, AMOUNT_PER_HOUR, AMOUNT_PER_DAY,
        ATM_PER_DAY, ECOMM_PER_DAY, CONTACTLESS_PER_DAY, PIN_FAILURES,
        MERCHANT_PER_DAY, COUNTRY_PER_DAY
    }
}
