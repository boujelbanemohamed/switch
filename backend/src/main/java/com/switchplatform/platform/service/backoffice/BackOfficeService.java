package com.switchplatform.platform.service.backoffice;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.backoffice.AuditLog;
import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import com.switchplatform.platform.model.backoffice.Report;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.fraud.FraudAlert;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.backoffice.AuditLogRepository;
import com.switchplatform.platform.repository.backoffice.MonitoringEventRepository;
import com.switchplatform.platform.repository.backoffice.ReportRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.fraud.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
    private final TransactionRepository transactionRepository;
    private final ClearingRecordRepository clearingRecordRepository;
    private final FraudAlertRepository fraudAlertRepository;

    @Value("${switch.reports.directory:./reports}")
    private String reportsDirectory;

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
        if (resourceType != null || resourceId != null) {
            return auditLogRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .stream()
                    .filter(l -> (resourceType == null || resourceType.equals(l.getResourceType()))
                            && (resourceId == null || resourceId.equals(l.getResourceId())))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return auditLogRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public List<AuditLog> getAuditLogsByUser(String userId, int limit) {
        return auditLogRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
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
        report.setStatus(Report.Status.GENERATING);
        reportRepository.save(report);

        try {
            String csv = generateCsv(report);
            String fileName = report.getReportType().name().toLowerCase() + "_"
                    + LocalDate.now() + "_" + reportId.toString().substring(0, 8) + ".csv";
            Path dir = Path.of(reportsDirectory);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            try (FileWriter fw = new FileWriter(filePath.toFile())) {
                fw.write(csv);
            }
            report.setFilePath(filePath.toAbsolutePath().toString());
            report.setFileFormat(Report.FileFormat.CSV);
            report.setStatus(Report.Status.COMPLETED);
            report.setGeneratedAt(OffsetDateTime.now());
            log.info("Report generated: {} -> {}", reportId, filePath);
        } catch (Exception e) {
            log.error("Report generation failed: {}", reportId, e);
            report.setStatus(Report.Status.FAILED);
            report.setErrorMessage(e.getMessage());
        }

        return reportRepository.save(report);
    }

    private String generateCsv(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Report: ").append(report.getName()).append("\n");
        sb.append("Type: ").append(report.getReportType()).append("\n");
        sb.append("Generated: ").append(OffsetDateTime.now()).append("\n\n");

        switch (report.getReportType()) {
            case TRANSACTION -> appendTransactionCsv(sb);
            case SETTLEMENT -> appendSettlementCsv(sb);
            case FRAUD -> appendFraudCsv(sb);
            case AUDIT -> appendAuditCsv(sb);
            default -> appendDefaultCsv(sb, report);
        }
        return sb.toString();
    }

    private void appendTransactionCsv(StringBuilder sb) {
        sb.append("Transaction ID,Timestamp,Type,Amount,Currency,Status\n");
        transactionRepository.findAll(PageRequest.of(0, 1000,
                Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()
            .forEach(t -> sb.append(safe(t.getId())).append(",")
                .append(safe(t.getCreatedAt())).append(",")
                .append(safe(t.getMessageType())).append(",")
                .append(safe(t.getAmount())).append(",")
                .append(safe(t.getCurrencyCode())).append(",")
                .append(safe(t.getStatus())).append("\n"));
    }

    private void appendSettlementCsv(StringBuilder sb) {
        sb.append("Clearing ID,Date,Transaction ID,Amount,Fee,Status\n");
        clearingRecordRepository.findAll(PageRequest.of(0, 1000,
                Sort.by(Sort.Direction.DESC, "clearingDate"))).getContent()
            .forEach(r -> sb.append(safe(r.getId())).append(",")
                .append(safe(r.getClearingDate())).append(",")
                .append(safe(r.getTransactionId())).append(",")
                .append(safe(r.getAmount())).append(",")
                .append(safe(r.getFeeAmount())).append(",")
                .append(safe(r.getStatus())).append("\n"));
    }

    private void appendFraudCsv(StringBuilder sb) {
        sb.append("Alert ID,Timestamp,Transaction ID,Score,Type,Status\n");
        fraudAlertRepository.findAll(PageRequest.of(0, 1000,
                Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()
            .forEach(a -> sb.append(safe(a.getId())).append(",")
                .append(safe(a.getCreatedAt())).append(",")
                .append(safe(a.getTransactionId())).append(",")
                .append(safe(a.getScore())).append(",")
                .append(safe(a.getAlertType())).append(",")
                .append(safe(a.getStatus())).append("\n"));
    }

    private String safe(Object o) {
        if (o == null) return "";
        return o.toString().replace(",", ";").replace("\n", " ");
    }

    private void appendAuditCsv(StringBuilder sb) {
        sb.append("ID,Timestamp,User,Action,Resource Type,Resource ID,Status,IP Address\n");
        List<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        for (AuditLog log : logs) {
            sb.append(log.getId()).append(",")
              .append(log.getCreatedAt()).append(",")
              .append(log.getUserId()).append(",")
              .append(log.getAction()).append(",")
              .append(log.getResourceType()).append(",")
              .append(log.getResourceId()).append(",")
              .append(log.getStatus()).append(",")
              .append(log.getIpAddress()).append("\n");
        }
    }

    private void appendDefaultCsv(StringBuilder sb, Report report) {
        sb.append("Report ID,Name,Type,Status,Created\n");
        sb.append(report.getId()).append(",")
          .append(report.getName()).append(",")
          .append(report.getReportType()).append(",")
          .append(report.getStatus()).append(",")
          .append(report.getCreatedAt()).append("\n");
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
