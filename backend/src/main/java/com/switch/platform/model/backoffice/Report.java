package com.switch.platform.model.backoffice;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "report_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(columnDefinition = "JSONB")
    private String parameters;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "file_format", length = 10)
    @Enumerated(EnumType.STRING)
    private FileFormat fileFormat;

    @Column(name = "generated_by", length = 64)
    private String generatedBy;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private Boolean scheduled = false;

    @Column(name = "schedule_cron", length = 64)
    private String scheduleCron;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum ReportType {
        TRANSACTION, SETTLEMENT, FRAUD, AUDIT, REGULATORY, PERFORMANCE, FINANCIAL, CUSTOM
    }

    public enum FileFormat {
        CSV, PDF, XLSX, JSON, XML
    }

    public enum Status {
        PENDING, GENERATING, COMPLETED, FAILED
    }
}
