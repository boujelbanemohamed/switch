package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pin_management")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(length = 64)
    private String pan;

    @Column(name = "pin_hash", columnDefinition = "TEXT")
    private String pinHash;

    @Column(name = "pin_format", length = 20)
    private String pinFormat;

    @Builder.Default
    @Column(name = "pin_type", length = 20)
    @Enumerated(EnumType.STRING)
    private PinType pinType = PinType.TRANSACTION;

    @Column(name = "pin_block", length = 64)
    private String pinBlock;

    @Builder.Default
    @Column(name = "pin_attempts")
    private Integer pinAttempts = 0;

    @Builder.Default
    @Column(name = "max_attempts")
    private Integer maxAttempts = 3;

    @Column(name = "last_attempt")
    private OffsetDateTime lastAttempt;

    @Column(name = "blocked_until")
    private OffsetDateTime blockedUntil;

    @Column(name = "last_changed")
    private OffsetDateTime lastChanged;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum PinType {
        TRANSACTION, ADMIN, PHONE
    }
}
