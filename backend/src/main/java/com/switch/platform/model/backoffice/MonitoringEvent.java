package com.switch.platform.model.backoffice;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monitoring_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    @Column(length = 64)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "metric_name", length = 128)
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(columnDefinition = "JSONB")
    private String details;

    @Column
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum Severity {
        INFO, WARNING, ERROR, CRITICAL
    }
}
