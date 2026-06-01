package com.switchplatform.platform.model.merchant;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_code", length = 15, nullable = false)
    private String merchantCode;

    @Column(name = "url", length = 512, nullable = false)
    private String url;

    @Column(name = "event_types", columnDefinition = "TEXT")
    private String eventTypes;

    @Column(name = "secret", length = 128)
    private String secret;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
