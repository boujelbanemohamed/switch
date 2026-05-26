package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(nullable = false, length = 128)
    private String token;

    @Column(name = "token_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "wallet_provider", length = 20)
    @Enumerated(EnumType.STRING)
    private WalletProvider walletProvider;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(length = 128)
    private String cryptogram;

    @Column(name = "token_expiry")
    private LocalDate tokenExpiry;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TokenStatus status = TokenStatus.ACTIVE;

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

    public enum TokenType {
        DEVICE, SERVER, MERCHANT
    }

    public enum WalletProvider {
        APPLE_PAY, GOOGLE_PAY, SAMSUNG_PAY, OTHER
    }

    public enum TokenStatus {
        ACTIVE, SUSPENDED, TERMINATED
    }
}
