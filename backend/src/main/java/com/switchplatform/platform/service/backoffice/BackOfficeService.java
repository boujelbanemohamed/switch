package com.switchplatform.platform.service.backoffice;

import com.switchplatform.platform.model.backoffice.AuditLog;
import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import com.switchplatform.platform.model.backoffice.Report;
import com.switchplatform.platform.repository.backoffice.AuditLogRepository;
import com.switchplatform.platform.repository.backoffice.MonitoringEventRepository;
import com.switchplatform.platform.repository.backoffice.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackOfficeService {

    private final AuditLogRepository auditLogRepository;
    private final ReportRepository reportRepository;
    private final MonitoringEventRepository monitoringEventRepository;

    public AuditLog logAudit(AuditLog logEntry) {
        if (logEntry.getStatus() == null) {
            logEntry.setStatus(AuditLog.Status.SUCCESS);
        }
        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("Audit log created: {} {} on {}", saved.getAction(),
                saved.getResourceType(), saved.getCreatedAt());
        return saved;
    }

    public List<AuditLog> getAuditLogs(String resourceType, String resourceId, int limit) {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(l -> (resourceType == null || resourceType.equals(l.getResourceType()))
                        && (resourceId == null || resourceId.equals(l.getResourceId())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AuditLog> getAuditLogsByUser(String userId, int limit) {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(l -> userId.equals(l.getUserId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Report createReport(Report report) {
        report.setId(null);
        report.setStatus(Report.Status.PENDING);
        Report saved = reportRepository.save(report);
        log.info("Report created: {} ({})", saved.getName(), saved.getReportType());
        return saved;
    }

    public Report generateReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setStatus(Report.Status.COMPLETED);
        report.setGeneratedAt(OffsetDateTime.now());
        Report saved = reportRepository.save(report);
        log.info("Report generated: {}", reportId);
        return saved;
    }

    public List<Report> getReportsByType(String type) {
        Report.ReportType reportType = Report.ReportType.valueOf(type);
        return reportRepository.findByReportType(reportType).stream()
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
                .build();

        MonitoringEvent saved = monitoringEventRepository.save(event);
        log.info("Monitoring event created: {} {} from {}", eventType, severity, source);
        return saved;
    }

    public MonitoringEvent acknowledgeEvent(Long eventId, String acknowledgedBy) {
        MonitoringEvent event = monitoringEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Monitoring event not found: " + eventId));
        event.setAcknowledged(true);
        event.setAcknowledgedBy(acknowledgedBy);
        event.setAcknowledgedAt(OffsetDateTime.now());
        MonitoringEvent saved = monitoringEventRepository.save(event);
        log.info("Monitoring event {} acknowledged by {}", eventId, acknowledgedBy);
        return saved;
    }

    public List<MonitoringEvent> getActiveAlerts() {
        List<MonitoringEvent> critical = monitoringEventRepository
                .findBySeverityOrderByCreatedAtDesc(MonitoringEvent.Severity.CRITICAL,
                        PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<MonitoringEvent> error = monitoringEventRepository
                .findBySeverityOrderByCreatedAtDesc(MonitoringEvent.Severity.ERROR,
                        PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        return Stream.concat(critical.stream(), error.stream())
                .filter(e -> !e.getAcknowledged())
                .sorted(Comparator.comparing(MonitoringEvent::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Map<MonitoringEvent.Severity, Long> getMonitoringStats(long minutes) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(minutes);
        List<MonitoringEvent> events = monitoringEventRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(cutoff, OffsetDateTime.now(),
                        PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        return events.stream()
                .collect(Collectors.groupingBy(
                        MonitoringEvent::getSeverity, Collectors.counting()));
    }
}
