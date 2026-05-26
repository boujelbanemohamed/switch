package com.switch.platform.model.fraud;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "behavioral_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehavioralProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cardholder_id", nullable = false, unique = true)
    private UUID cardholderId;

    @Column(name = "avg_transaction_amount", precision = 18, scale = 3)
    private BigDecimal avgTransactionAmount;

    @Column(name = "avg_transactions_per_day", precision = 10, scale = 2)
    private BigDecimal avgTransactionsPerDay;

    @Column(name = "typical_merchant_categories", columnDefinition = "TEXT[]")
    private String[] typicalMerchantCategories;

    @Column(name = "typical_countries", columnDefinition = "TEXT[]")
    private String[] typicalCountries;

    @Column(name = "typical_hours", columnDefinition = "INTEGER[]")
    private Integer[] typicalHours;

    @Column(name = "typical_days", columnDefinition = "INTEGER[]")
    private Integer[] typicalDays;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

    @Column(name = "profile_data", columnDefinition = "JSONB")
    private String profileData;

    @Column(name = "model_version", length = 32)
    private String modelVersion;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
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
