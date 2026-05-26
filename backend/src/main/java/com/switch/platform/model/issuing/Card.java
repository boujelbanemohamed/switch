package com.switch.platform.model.issuing;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "card_account_id", nullable = false)
    private UUID cardAccountId;

    @Column(name = "card_number_hash", length = 128)
    private String cardNumberHash;

    @Column(name = "card_number_suffix", length = 4)
    private String cardNumberSuffix;

    @Column(name = "card_type", length = 20)
    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Column(name = "card_brand", length = 20)
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand;

    @Column(name = "card_network", length = 20)
    @Enumerated(EnumType.STRING)
    private CardNetwork cardNetwork;

    @Column(name = "product_code", length = 32)
    private String productCode;

    @Column(name = "embossed_line_1", length = 100)
    private String embossedLine1;

    @Column(name = "embossed_line_2", length = 100)
    private String embossedLine2;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "cvv_hash", length = 128)
    private String cvvHash;

    @Column(name = "pin_block", length = 64)
    private String pinBlock;

    @Column(name = "pin_attempts")
    private Integer pinAttempts = 0;

    @Column(name = "pin_max_attempts")
    private Integer pinMaxAttempts = 3;

    @Column(name = "pin_last_updated")
    private OffsetDateTime pinLastUpdated;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.PENDING_ACTIVATION;

    @Column(name = "status_reason", length = 255)
    private String statusReason;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "block_date")
    private LocalDate blockDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Column(name = "expiry_notification_sent")
    private Boolean expiryNotificationSent = false;

    @Column(name = "is_renewable")
    private Boolean isRenewable = true;

    @Column(name = "is_reissuable")
    private Boolean isReissuable = true;

    @Column(name = "reissue_reason", length = 255)
    private String reissueReason;

    @Column
    private Boolean embossed = false;

    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled = true;

    @Column(name = "online_enabled")
    private Boolean onlineEnabled = true;

    @Column(name = "international_enabled")
    private Boolean internationalEnabled = true;

    @Column(name = "ecommerce_enabled")
    private Boolean ecommerceEnabled = true;

    @Column(name = "atm_enabled")
    private Boolean atmEnabled = true;

    @Column(name = "mag_stripe_enabled")
    private Boolean magStripeEnabled = true;

    @Column(name = "chip_enabled")
    private Boolean chipEnabled = true;

    @Column(name = "daily_limit", precision = 18, scale = 3)
    private BigDecimal dailyLimit;

    @Column(name = "weekly_limit", precision = 18, scale = 3)
    private BigDecimal weeklyLimit;

    @Column(name = "monthly_limit", precision = 18, scale = 3)
    private BigDecimal monthlyLimit;

    @Column(name = "single_txn_limit", precision = 18, scale = 3)
    private BigDecimal singleTxnLimit;

    @Column(name = "requestor_reference", length = 64)
    private String requestorReference;

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

    public enum CardType {
        DEBIT, CREDIT, PREPAID, CHARGE, VIRTUAL
    }

    public enum CardBrand {
        VISA, MASTERCARD, AMEX, CB, VERVE, OTHER
    }

    public enum CardNetwork {
        VISA_NET, MASTERCARD_NET, CB_NET, AMEX_NET, VERVE_NET
    }

    public enum CardStatus {
        PENDING_ACTIVATION, ACTIVE, INACTIVE, BLOCKED, SUSPENDED, EXPIRED,
        STOLEN, LOST, DAMAGED, CLOSED, RENEWED
    }
}
