package com.switchplatform.platform.model.authorization;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_holds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "card_id", length = 64)
    private String cardId;

    @Column(name = "card_account_id", length = 64)
    private String cardAccountId;

    @Column(precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;
}
