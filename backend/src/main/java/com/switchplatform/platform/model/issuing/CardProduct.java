package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardProduct {

    public enum CardType {
        DEBIT, CREDIT, PREPAID, CHARGE, VIRTUAL
    }

    public enum CardBrand {
        VISA, MASTERCARD, AMEX, CB, VERVE, OTHER
    }

    public enum CardNetwork {
        VISA_NET, MASTERCARD_NET, CB_NET, AMEX_NET, VERVE_NET
    }

    public enum Status {
        DRAFT, ACTIVE, INACTIVE, ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "product_code", nullable = false, length = 20, unique = true)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", nullable = false, length = 20)
    private CardBrand cardBrand;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", length = 20)
    private CardNetwork cardNetwork;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled;

    @Column(name = "online_enabled")
    private Boolean onlineEnabled;

    @Column(name = "international_enabled")
    private Boolean internationalEnabled;

    @Column(name = "ecommerce_enabled")
    private Boolean ecommerceEnabled;

    @Column(name = "atm_enabled")
    private Boolean atmEnabled;

    @Column(name = "mag_stripe_enabled")
    private Boolean magStripeEnabled;

    @Column(name = "chip_enabled")
    private Boolean chipEnabled;

    @Column(name = "is_renewable")
    private Boolean isRenewable;

    @Column(name = "is_reissuable")
    private Boolean isReissuable;

    @Column(name = "is_virtual_supported")
    private Boolean isVirtualSupported;

    @Column(name = "daily_limit", precision = 18, scale = 3)
    private BigDecimal dailyLimit;

    @Column(name = "weekly_limit", precision = 18, scale = 3)
    private BigDecimal weeklyLimit;

    @Column(name = "monthly_limit", precision = 18, scale = 3)
    private BigDecimal monthlyLimit;

    @Column(name = "single_txn_limit", precision = 18, scale = 3)
    private BigDecimal singleTxnLimit;

    @Column(name = "annual_fee", precision = 18, scale = 3)
    private BigDecimal annualFee;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
