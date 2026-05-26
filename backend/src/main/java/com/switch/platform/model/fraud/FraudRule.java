package com.switch.platform.model.fraud;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RuleCategory ruleCategory;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.MEDIUM;

    @Column(nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private Action action = Action.FLAG;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "score_weight")
    private Integer scoreWeight;

    @Column(name = "cooldown_seconds")
    private Integer cooldownSeconds;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "false_positive_count")
    private Integer falsePositiveCount = 0;

    @Column(name = "true_positive_count")
    private Integer truePositiveCount = 0;

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

    public enum RuleCategory {
        VELOCITY, GEO, BEHAVIORAL, AMOUNT, MERCHANT, DEVICE, NETWORK, ML_MODEL, MANUAL
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Action {
        BLOCK, FLAG, CHALLENGE, MONITOR, TFA, ALLOW
    }

    public enum Status {
        ACTIVE, INACTIVE, TESTING
    }
}
