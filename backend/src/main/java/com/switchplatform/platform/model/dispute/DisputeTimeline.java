package com.switchplatform.platform.model.dispute;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispute_timeline")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column(name = "performed_by", length = 64)
    private String performedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
