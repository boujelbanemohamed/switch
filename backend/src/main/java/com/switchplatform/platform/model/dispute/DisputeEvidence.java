package com.switchplatform.platform.model.dispute;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispute_evidence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEvidence {

    public enum EvidenceType {
        RECEIPT, CONTRACT, COMMUNICATION, DELIVERY_PROOF,
        REFUND_PROOF, OTHER_DOCUMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    @Column(name = "submitted_by", nullable = false, length = 20)
    private String submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 30)
    private EvidenceType evidenceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_reference", length = 256)
    private String fileReference;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;
}
