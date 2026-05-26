package com.switchplatform.platform.model.acquiring;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mdr_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MdrPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "transaction_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "domestic_rate", precision = 6, scale = 4)
    private BigDecimal domesticRate;

    @Column(name = "international_rate", precision = 6, scale = 4)
    private BigDecimal internationalRate;

    @Column(name = "fixed_fee_domestic", precision = 18, scale = 3)
    private BigDecimal fixedFeeDomestic;

    @Column(name = "fixed_fee_international", precision = 18, scale = 3)
    private BigDecimal fixedFeeInternational;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MdrPlanStatus status = MdrPlanStatus.ACTIVE;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum TransactionType {
        PURCHASE, REFUND, CHARGEBACK, WITHDRAWAL
    }

    public enum MdrPlanStatus {
        ACTIVE, INACTIVE
    }
}
