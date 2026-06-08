package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_programs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "earning_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal earningRate;

    @Column(name = "point_value", nullable = false, precision = 10, scale = 6)
    private BigDecimal pointValue;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProgramStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum ProgramStatus {
        ACTIVE, INACTIVE
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = ProgramStatus.ACTIVE;
        if (earningRate == null) earningRate = new BigDecimal("0.1");
        if (pointValue == null) pointValue = new BigDecimal("0.01");
        if (currency == null) currency = "TND";
    }
}
