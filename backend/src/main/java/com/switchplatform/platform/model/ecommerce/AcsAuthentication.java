package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acs_authentications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcsAuthentication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "merchant_name", length = 128)
    private String merchantName;

    @Column(precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "pan_hash", length = 128)
    private String panHash;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "authentication_value", length = 128)
    private String authenticationValue;

    @Column(length = 2)
    private String eci;

    @Column(name = "ds_trans_id")
    private UUID dsTransId;

    @Column(name = "three_ds_version", length = 10)
    private String threeDsVersion;

    @Column(name = "ds_url", length = 512)
    private String dsUrl;

    @Column(name = "acs_url", length = 512)
    private String acsUrl;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_decision", length = 20)
    private String riskDecision;

    @Column(name = "challenge_canceled")
    private Boolean challengeCanceled;

    @Column(name = "whitelist_status", length = 20)
    private String whitelistStatus;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.CREATED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Status {
        CREATED, CHALLENGE_REQUIRED, AUTHENTICATED, FAILED, DECLINED, EXPIRED
    }
}
