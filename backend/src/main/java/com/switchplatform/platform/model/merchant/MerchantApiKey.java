package com.switchplatform.platform.model.merchant;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_code", length = 15, nullable = false)
    private String merchantCode;

    @Column(name = "api_key", length = 64, nullable = false, unique = true)
    private String apiKey;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "permissions", length = 255)
    private String permissions;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
