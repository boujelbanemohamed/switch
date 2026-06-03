package com.switchplatform.platform.model.clearing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "multilateral_netting_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultilateralNettingSession {

    public enum Status {
        CALCULATING, CALCULATED, CONFIRMED, SETTLED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.CALCULATING;

    @Column(name = "total_gross_amount", precision = 18, scale = 3)
    private BigDecimal totalGrossAmount;

    @Column(name = "total_net_amount", precision = 18, scale = 3)
    private BigDecimal totalNetAmount;

    @Column(name = "netting_efficiency", precision = 5, scale = 2)
    private BigDecimal nettingEfficiency;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
