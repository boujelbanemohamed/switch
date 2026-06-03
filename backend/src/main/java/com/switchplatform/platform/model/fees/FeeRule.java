package com.switchplatform.platform.model.fees;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeRule {

    public enum CalcMethod {
        FLAT, PERCENTAGE, TIERED, MIXED, INTERCHANGE_LOOKUP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "rule_order", nullable = false)
    private Integer ruleOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "calc_method", nullable = false, length = 20)
    private CalcMethod calcMethod;

    @Column(name = "flat_amount", precision = 18, scale = 3)
    private BigDecimal flatAmount;

    @Column(name = "percentage_rate", precision = 10, scale = 6)
    private BigDecimal percentageRate;

    @Column(name = "min_amount", precision = 18, scale = 3)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 3)
    private BigDecimal maxAmount;

    @Column(name = "min_tx_amount", precision = 18, scale = 3)
    private BigDecimal minTxAmount;

    @Column(name = "max_tx_amount", precision = 18, scale = 3)
    private BigDecimal maxTxAmount;

    @Column(name = "brand_filter", length = 20)
    private String brandFilter;

    @Column(name = "card_type_filter", length = 20)
    private String cardTypeFilter;

    @Column(name = "mcc_filter", length = 4)
    private String mccFilter;

    @Column(name = "region_filter", length = 4)
    private String regionFilter;

    @Column(name = "entry_mode_filter", length = 20)
    private String entryModeFilter;

    @Column(name = "is_waivable")
    private Boolean isWaivable;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
