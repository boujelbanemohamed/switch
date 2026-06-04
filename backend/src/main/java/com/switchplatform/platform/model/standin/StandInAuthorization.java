package com.switchplatform.platform.model.standin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stand_in_authorizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandInAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "card_suffix", length = 4)
    private String cardSuffix;

    @Column(name = "issuer_participant_id")
    private UUID issuerParticipantId;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Decision decision;

    @Column(length = 100)
    private String reason;

    @Column(nullable = false)
    private Boolean reconciled;

    @Column(name = "authorized_at", nullable = false)
    private OffsetDateTime authorizedAt;

    @PrePersist
    protected void onCreate() {
        authorizedAt = OffsetDateTime.now();
        if (reconciled == null) reconciled = false;
    }

    public enum Decision {
        APPROVED, DECLINED
    }
}
