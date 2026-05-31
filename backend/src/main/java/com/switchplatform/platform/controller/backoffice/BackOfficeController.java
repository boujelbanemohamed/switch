package com.switchplatform.platform.controller.backoffice;

import com.switchplatform.platform.model.backoffice.AuditLog;
import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import com.switchplatform.platform.model.backoffice.Report;
import com.switchplatform.platform.service.backoffice.BackOfficeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice")
@RequiredArgsConstructor
@Validated
public class BackOfficeController {

    private final BackOfficeService backOfficeService;

    @PostMapping("/audit")
    public ResponseEntity<AuditLog> createAuditLog(@Valid @RequestBody AuditLog auditLog) {
        return ResponseEntity.ok(backOfficeService.logAudit(auditLog));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> queryAuditLogs(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(backOfficeService.getAuditLogs(resourceType, resourceId, limit));
    }

    @GetMapping("/audit/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(backOfficeService.getAuditLogsByUser(userId, limit));
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> createReport(@Valid @RequestBody Report report) {
        return ResponseEntity.ok(backOfficeService.createReport(report));
    }

    @PostMapping("/reports/{id}/generate")
    public ResponseEntity<Report> generateReport(@PathVariable UUID id) {
        return ResponseEntity.ok(backOfficeService.generateReport(id));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Report>> listReports(@RequestParam String type) {
        return ResponseEntity.ok(backOfficeService.getReportsByType(type));
    }

    @PostMapping("/monitoring/events")
    public ResponseEntity<MonitoringEvent> createMonitoringEvent(@Valid @RequestBody Map<String, Object> body) {
        String eventType = (String) body.get("eventType");
        String severity = (String) body.get("severity");
        String source = (String) body.get("source");
        String message = (String) body.get("message");
        Double metricValue = body.get("metricValue") != null
                ? ((Number) body.get("metricValue")).doubleValue() : null;
        Double threshold = body.get("threshold") != null
                ? ((Number) body.get("threshold")).doubleValue() : null;
        return ResponseEntity.ok(backOfficeService.createMonitoringEvent(
                eventType, severity, source, message, metricValue, threshold));
    }

    @PostMapping("/monitoring/events/{id}/acknowledge")
    public ResponseEntity<MonitoringEvent> acknowledgeEvent(
            @PathVariable Long id, @RequestParam String by) {
        return ResponseEntity.ok(backOfficeService.acknowledgeEvent(id, by));
    }

    @GetMapping("/monitoring/alerts")
    public ResponseEntity<List<MonitoringEvent>> getActiveAlerts() {
        return ResponseEntity.ok(backOfficeService.getActiveAlerts());
    }

    @GetMapping("/monitoring/stats")
    public ResponseEntity<Map<MonitoringEvent.Severity, Long>> getMonitoringStats(
            @RequestParam(defaultValue = "60") long minutes) {
        return ResponseEntity.ok(backOfficeService.getMonitoringStats(minutes));
    }
}
