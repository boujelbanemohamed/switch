package com.switch.platform.model.fraud;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "alert_type", length = 64)
    private String alertType;

    @Column(length = 20)
    private String severity;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    @Column(nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private Decision decision = Decision.REVIEW;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum Status {
        OPEN, INVESTIGATING, CONFIRMED, FALSE_POSITIVE, CLOSED
    }

    public enum Decision {
        APPROVED, DECLINED, REVIEW, ESCALATED
    }
}
