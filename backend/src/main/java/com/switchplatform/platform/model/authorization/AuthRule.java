package com.switchplatform.platform.model.authorization;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ActionType action = ActionType.APPROVE;

    @Column(name = "response_code", length = 2)
    private String responseCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_expression", columnDefinition = "JSONB")
    private String conditionExpression;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "merchant_category", length = 4)
    private String merchantCategory;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "time_start")
    private LocalTime timeStart;

    @Column(name = "time_end")
    private LocalTime timeEnd;

    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private RuleStatus status = RuleStatus.ACTIVE;

    @Column(name = "apply_to_all")
    private Boolean applyToAll = false;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

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

    public enum RuleType {
        SOLDE, LIMIT, VELOCITY, FRAUD, RISK, MERCHANT, PRODUCT, GEO, TIME, CUSTOM
    }

    public enum ActionType {
        APPROVE, DECLINE, CHALLENGE, REVIEW, TFA, PIN
    }

    public enum RuleStatus {
        ACTIVE, INACTIVE, TEST
    }
}
