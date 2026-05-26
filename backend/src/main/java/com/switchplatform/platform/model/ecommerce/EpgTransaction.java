package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "epg_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpgTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "merchant_transaction_id", nullable = false, length = 128)
    private String merchantTransactionId;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "pan_hash", length = 128)
    private String panHash;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "cardholder_name", length = 128)
    private String cardholderName;

    @Column(name = "customer_email", length = 256)
    private String customerEmail;

    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @Column(name = "customer_ip", length = 45)
    private String customerIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_channel", length = 10)
    @Enumerated(EnumType.STRING)
    private DeviceChannel deviceChannel;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "three_ds_status", length = 30)
    private String threeDsStatus;

    @Column(name = "three_ds_required")
    private Boolean threeDsRequired;

    @Column(length = 128)
    private String cavv;

    @Column(length = 2)
    private String eci;

    @Column(length = 64)
    private String xid;

    @Column(name = "acs_transaction_id")
    private UUID acsTransactionId;

    @Column(name = "error_code", length = 10)
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "authorized_at")
    private OffsetDateTime authorizedAt;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.INITIATED;
        if (deviceChannel == null) deviceChannel = DeviceChannel.WEB;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Status {
        INITIATED, AUTHENTICATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED, CHARGEBACK, CANCELED
    }

    public enum DeviceChannel {
        WEB, MOBILE, TABLET, API
    }
}
