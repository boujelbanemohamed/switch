package com.switchplatform.platform.model.kyc;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument {

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "cardholder_id", nullable = false)
    private UUID cardholderId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "issuing_country", length = 2)
    private String issuingCountry;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "mime_type", length = 64)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (verificationStatus == null) verificationStatus = VerificationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
