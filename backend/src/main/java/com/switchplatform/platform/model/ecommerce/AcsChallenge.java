package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acs_challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcsChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "authentication_id", nullable = false)
    private UUID authenticationId;

    @Column(name = "challenge_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChallengeType challengeType;

    @Column(name = "challenge_data", columnDefinition = "TEXT")
    private String challengeData;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private Integer attempts;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.PENDING;
        if (attempts == null) attempts = 0;
        if (maxAttempts == null) maxAttempts = 3;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ChallengeType {
        OTP, SMS, EMAIL, BIOMETRIC, APP_NOTIFICATION, PASSWORD
    }

    public enum Status {
        PENDING, SENT, VERIFIED, FAILED, EXPIRED
    }
}
