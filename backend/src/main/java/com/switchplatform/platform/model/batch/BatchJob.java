package com.switchplatform.platform.model.batch;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJob {

    public enum JobType {
        EOD_CLEARING, BOD_POSITIONS, SETTLEMENT_FILE,
        RECONCILIATION, INTERCHANGE_CALC, REPORT_GENERATION,
        NOTIFICATION_DIGEST, EXPIRY_CHECK, DISPUTE_DEADLINE_CHECK
    }

    public enum Status {
        SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SCHEDULED;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "result_summary", columnDefinition = "JSONB")
    private String resultSummary;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
