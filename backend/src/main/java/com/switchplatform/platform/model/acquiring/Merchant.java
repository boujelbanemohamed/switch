package com.switchplatform.platform.model.acquiring;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.switchplatform.platform.model.Participant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 15)
    private String merchantId;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Column(name = "trading_name", length = 255)
    private String tradingName;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MerchantStatus status = MerchantStatus.PENDING_ONBOARDING;

    @Column(name = "risk_level", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.STANDARD;

    @Column(name = "onboarding_date")
    private OffsetDateTime onboardingDate;

    @Column(name = "activation_date")
    private OffsetDateTime activationDate;

    @Column(name = "termination_date")
    private OffsetDateTime terminationDate;

    @Column(name = "settlement_method", length = 20)
    @Enumerated(EnumType.STRING)
    private SettlementMethod settlementMethod;

    @Column(name = "settlement_currency", length = 3)
    private String settlementCurrency;

    @Column(name = "settlement_account_iban", length = 34)
    private String settlementAccountIban;

    @Column(name = "settlement_cycle", length = 10)
    @Enumerated(EnumType.STRING)
    private SettlementCycle settlementCycle;

    @Column(name = "mdr_percentage", precision = 6, scale = 4)
    private BigDecimal mdrPercentage;

    @Column(name = "mdr_fixed_fee", precision = 18, scale = 3)
    private BigDecimal mdrFixedFee;

    @Column(name = "mdr_plan_id")
    private UUID mdrPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquiring_participant_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Participant acquiringParticipant;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (onboardingDate == null) onboardingDate = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum MerchantStatus {
        PENDING_ONBOARDING, ACTIVE, SUSPENDED, TERMINATED, UNDER_REVIEW
    }

    public enum RiskLevel {
        LOW, STANDARD, MEDIUM, HIGH
    }

    public enum SettlementMethod {
        TARGET, DIRECT, NETTING, GROSS
    }

    public enum SettlementCycle {
        D, D1, D2, WEEKLY, BIWEEKLY, MONTHLY
    }
}
