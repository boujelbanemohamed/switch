package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cof_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CofToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pan_display", nullable = false, length = 8)
    private String panDisplay;

    @Column(name = "pan_reference", nullable = false, length = 64)
    private String panReference;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "cardholder_name", length = 100)
    private String cardholderName;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (tokenType == null) tokenType = "UNSCHEDULED";
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
