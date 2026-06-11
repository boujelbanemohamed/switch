package com.switchplatform.platform.model.issuing;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cardholders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cardholder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", length = 64, unique = true)
    private String externalId;

    @Column(length = 10)
    private String title;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String mobile;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 3)
    @Size(max = 3, message = "countryCode must be at most 3 characters")
    private String countryCode;

    @Column(length = 3)
    @Size(max = 3, message = "nationality must be at most 3 characters")
    private String nationality;

    @Column(name = "id_document_type", length = 20)
    @Enumerated(EnumType.STRING)
    private IdDocumentType idDocumentType;

    @Column(name = "id_document_number", length = 50)
    private String idDocumentNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardholderStatus status = CardholderStatus.ACTIVE;

    @Column(name = "kyc_level")
    private Integer kycLevel;

    @Column(name = "risk_profile", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskProfile riskProfile = RiskProfile.STANDARD;

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

    public enum IdDocumentType {
        PASSPORT, NATIONAL_ID, DRIVING_LICENSE, RESIDENCE
    }

    public enum CardholderStatus {
        ACTIVE, INACTIVE, BLOCKED, DECEASED
    }

    public enum RiskProfile {
        LOW, STANDARD, MEDIUM, HIGH, VERY_HIGH
    }
}
