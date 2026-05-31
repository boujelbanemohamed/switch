package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acs_card_enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcsCardEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "cardholder_id")
    private UUID cardholderId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "card_number_hash", length = 128)
    private String cardNumberHash;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.ENROLLED;
        if (enrolledAt == null) enrolledAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Status {
        ENROLLED, ACTIVE, SUSPENDED, CANCELLED
    }
}
