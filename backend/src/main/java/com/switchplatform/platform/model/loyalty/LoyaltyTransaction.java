package com.switchplatform.platform.model.loyalty;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal points;

    @Column(name = "transaction_ref", length = 64)
    private String transactionRef;

    @Column(length = 255)
    private String description;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(precision = 18, scale = 3)
    private BigDecimal remaining;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum TransactionType {
        EARN, BURN, ADJUST, EXPIRE
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
