package com.switchplatform.platform.model.acquiring;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "terminals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "terminal_id", nullable = false, unique = true, length = 8)
    private String terminalId;

    @Column(name = "serial_number", length = 64)
    private String serialNumber;

    @Column(name = "terminal_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TerminalType terminalType;

    @Column(length = 50)
    private String manufacturer;

    @Column(length = 50)
    private String model;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TerminalStatus status = TerminalStatus.ACTIVE;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "location_address", length = 255)
    private String locationAddress;

    @Column(length = 100)
    private String city;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "contactless_supported")
    private Boolean contactlessSupported = true;

    @Column(name = "chip_supported")
    private Boolean chipSupported = true;

    @Column(name = "mag_stripe_supported")
    private Boolean magStripeSupported = true;

    @Column(name = "pin_supported")
    private Boolean pinSupported = true;

    @Column(name = "supported_card_brands", columnDefinition = "VARCHAR(20)[]")
    private String[] supportedCardBrands;

    @Column(name = "supported_currencies", columnDefinition = "VARCHAR(3)[]")
    private String[] supportedCurrencies;

    @Column(name = "encryption_key_id", length = 64)
    private String encryptionKeyId;

    @Column(name = "m_key", length = 64)
    private String mKey;

    @Column(length = 64)
    private String pik;

    @Column(length = 64)
    private String mak;

    @Column(name = "last_activity_at")
    private OffsetDateTime lastActivityAt;

    @Column(name = "last_contact")
    private OffsetDateTime lastContact;

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

    public enum TerminalType {
        PHYSICAL_TPE, SOFT_POS, ECOMMERCE, MOTO, ATM, KIOSK, MOBILE
    }

    public enum TerminalStatus {
        ACTIVE, INACTIVE, SUSPENDED, RETIRED, MALFUNCTION
    }
}
