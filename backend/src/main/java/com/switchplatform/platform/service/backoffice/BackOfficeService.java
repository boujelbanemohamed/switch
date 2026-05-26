package com.switchplatform.platform.service.backoffice;

import com.switchplatform.platform.model.backoffice.AuditLog;
import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import com.switchplatform.platform.model.backoffice.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackOfficeService {

    private final Map<Long, AuditLog> auditLogs = new ConcurrentHashMap<>();
    private final Map<UUID, Report> reports = new ConcurrentHashMap<>();
    private final Map<Long, MonitoringEvent> monitoringEvents = new ConcurrentHashMap<>();
    private final AtomicLong auditIdSeq = new AtomicLong(0);
    private final AtomicLong eventIdSeq = new AtomicLong(0);

    public AuditLog logAudit(AuditLog logEntry) {
        long id = auditIdSeq.incrementAndGet();
        logEntry.setId(id);
        if (logEntry.getStatus() == null) {
            logEntry.setStatus(AuditLog.Status.SUCCESS);
        }
        logEntry.setCreatedAt(OffsetDateTime.now());
        auditLogs.put(id, logEntry);
        log.info("Audit log created: {} {} on {}", logEntry.getAction(),
                logEntry.getResourceType(), logEntry.getCreatedAt());
        return logEntry;
    }

    public List<AuditLog> getAuditLogs(String resourceType, String resourceId, int limit) {
        return auditLogs.values().stream()
                .filter(l -> (resourceType == null || resourceType.equals(l.getResourceType()))
                        && (resourceId == null || resourceId.equals(l.getResourceId())))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AuditLog> getAuditLogsByUser(String userId, int limit) {
        return auditLogs.values().stream()
                .filter(l -> userId.equals(l.getUserId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Report createReport(Report report) {
        report.setStatus(Report.Status.PENDING);
        report.setCreatedAt(OffsetDateTime.now());
        reports.put(report.getId(), report);
        log.info("Report created: {} ({})", report.getName(), report.getReportType());
        return report;
    }

    public Report generateReport(UUID reportId) {
        Report report = reports.get(reportId);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        report.setStatus(Report.Status.COMPLETED);
        report.setGeneratedAt(OffsetDateTime.now());
        log.info("Report generated: {}", reportId);
        return report;
    }

    public List<Report> getReportsByType(String type) {
        return reports.values().stream()
                .filter(r -> type.equals(r.getReportType().name()))
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public MonitoringEvent createMonitoringEvent(String eventType, String severity,
                                                  String source, String message,
                                                  Double metricValue, Double threshold) {
        MonitoringEvent event = MonitoringEvent.builder()
                .eventType(eventType)
                .severity(MonitoringEvent.Severity.valueOf(severity.toUpperCase()))
                .source(source)
                .message(message)
                .metricValue(metricValue)
                .thresholdValue(threshold)
                .acknowledged(false)
                .createdAt(OffsetDateTime.now())
                .build();

        long id = eventIdSeq.incrementAndGet();
        event.setId(id);
        monitoringEvents.put(id, event);
        log.info("Monitoring event created: {} {} from {}", eventType, severity, source);
        return event;
    }

    public MonitoringEvent acknowledgeEvent(Long eventId, String acknowledgedBy) {
        MonitoringEvent event = monitoringEvents.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Monitoring event not found: " + eventId);
        }
        event.setAcknowledged(true);
        event.setAcknowledgedBy(acknowledgedBy);
        event.setAcknowledgedAt(OffsetDateTime.now());
        log.info("Monitoring event {} acknowledged by {}", eventId, acknowledgedBy);
        return event;
    }

    public List<MonitoringEvent> getActiveAlerts() {
        return monitoringEvents.values().stream()
                .filter(e -> !e.getAcknowledged())
                .filter(e -> e.getSeverity() == MonitoringEvent.Severity.CRITICAL
                        || e.getSeverity() == MonitoringEvent.Severity.ERROR)
                .sorted(Comparator.comparing(MonitoringEvent::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Map<MonitoringEvent.Severity, Long> getMonitoringStats(long minutes) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(minutes);
        return monitoringEvents.values().stream()
                .filter(e -> e.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(
                        MonitoringEvent::getSeverity, Collectors.counting()));
    }
}
