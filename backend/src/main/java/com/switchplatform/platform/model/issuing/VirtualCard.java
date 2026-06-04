package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "virtual_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualCard {

    public enum Status {
        PENDING_ACTIVATION, ACTIVE, SUSPENDED, EXPIRED, CONSUMED, CANCELLED
    }

    public enum UsageType {
        SINGLE_USE, MULTI_USE, RECURRING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "funding_card_id")
    private UUID fundingCardId;

    @Column(name = "card_product_id")
    private UUID cardProductId;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "external_id", length = 64, unique = true)
    private String externalId;

    @Column(name = "pan_hash", nullable = false, length = 64)
    private String panHash;

    @Column(name = "pan_suffix", nullable = false, length = 4)
    private String panSuffix;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cvv_hash", length = 64)
    private String cvvHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private UsageType usageType;

    @Column(name = "name_on_card", length = 128)
    private String nameOnCard;

    @Column(name = "amount_limit", precision = 18, scale = 3)
    private BigDecimal amountLimit;

    @Column(name = "amount_used", precision = 18, scale = 3)
    private BigDecimal amountUsed;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "merchant_locked", length = 256)
    private String merchantLocked;

    @Column(name = "merchant_category", length = 4)
    private String merchantCategory;

    @Column(name = "mcc_locks", columnDefinition = "TEXT")
    private String mccLocks;

    @Column(name = "max_transactions")
    private Integer maxTransactions;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "single_use_amount", precision = 18, scale = 3)
    private BigDecimal singleUseAmount;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

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
        if (status == null) status = Status.PENDING_ACTIVATION;
        if (amountUsed == null) amountUsed = BigDecimal.ZERO;
        if (transactionCount == null) transactionCount = 0;
        if (externalId != null && externalId.isBlank()) externalId = null;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
