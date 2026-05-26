package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "epg_merchant_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpgMerchantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "api_key_hash", nullable = false, length = 256)
    private String apiKeyHash;

    @Column(name = "api_secret_hash", nullable = false, length = 256)
    private String apiSecretHash;

    @Column(name = "webhook_url", length = 512)
    private String webhookUrl;

    @Column(name = "callback_url", length = 512)
    private String callbackUrl;

    @Column(name = "allowed_currencies", columnDefinition = "VARCHAR(3)[]")
    private String allowedCurrencies;

    @Column(name = "allowed_card_brands", columnDefinition = "VARCHAR(20)[]")
    private String allowedCardBrands;

    @Column(name = "min_amount", precision = 18, scale = 3)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 3)
    private BigDecimal maxAmount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
