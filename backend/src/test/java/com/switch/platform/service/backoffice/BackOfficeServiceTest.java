package com.switch.platform.service.backoffice;

import com.switch.platform.model.backoffice.AuditLog;
import com.switch.platform.model.backoffice.MonitoringEvent;
import com.switch.platform.model.backoffice.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BackOfficeServiceTest {

    private BackOfficeService backOfficeService;

    @BeforeEach
    void setUp() {
        backOfficeService = new BackOfficeService();
    }

    @Test
    void shouldLogAudit() {
        AuditLog logEntry = AuditLog.builder()
                .userId("user1")
                .action("UPDATE")
                .resourceType("CARD")
                .resourceId("card-001")
                .oldValue("{\"status\":\"ACTIVE\"}")
                .newValue("{\"status\":\"BLOCKED\"}")
                .ipAddress("192.168.1.1")
                .build();

        AuditLog result = backOfficeService.logAudit(logEntry);

        assertNotNull(result.getId());
        assertEquals("user1", result.getUserId());
        assertEquals("UPDATE", result.getAction());
        assertEquals("CARD", result.getResourceType());
        assertEquals("card-001", result.getResourceId());
        assertEquals(AuditLog.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldGetAuditLogsByResource() {
        AuditLog log1 = AuditLog.builder().userId("user1").action("CREATE").resourceType("CARD").resourceId("card-001").build();
        AuditLog log2 = AuditLog.builder().userId("user2").action("UPDATE").resourceType("CARD").resourceId("card-001").build();
        AuditLog log3 = AuditLog.builder().userId("user3").action("DELETE").resourceType("USER").resourceId("user-001").build();

        backOfficeService.logAudit(log1);
        backOfficeService.logAudit(log2);
        backOfficeService.logAudit(log3);

        List<AuditLog> byType = backOfficeService.getAuditLogs("CARD", null, 10);
        assertEquals(2, byType.size());
        assertTrue(byType.stream().allMatch(l -> "CARD".equals(l.getResourceType())));

        List<AuditLog> byTypeAndId = backOfficeService.getAuditLogs("CARD", "card-001", 10);
        assertEquals(2, byTypeAndId.size());

        List<AuditLog> byIdOnly = backOfficeService.getAuditLogs(null, "user-001", 10);
        assertEquals(1, byIdOnly.size());
    }

    @Test
    void shouldGetAuditLogsByUser() {
        AuditLog log1 = AuditLog.builder().userId("alice").action("LOGIN").resourceType("SESSION").build();
        AuditLog log2 = AuditLog.builder().userId("alice").action("LOGOUT").resourceType("SESSION").build();
        AuditLog log3 = AuditLog.builder().userId("bob").action("LOGIN").resourceType("SESSION").build();

        backOfficeService.logAudit(log1);
        backOfficeService.logAudit(log2);
        backOfficeService.logAudit(log3);

        List<AuditLog> aliceLogs = backOfficeService.getAuditLogsByUser("alice", 10);
        assertEquals(2, aliceLogs.size());
        assertTrue(aliceLogs.stream().allMatch(l -> "alice".equals(l.getUserId())));

        List<AuditLog> bobLogs = backOfficeService.getAuditLogsByUser("bob", 10);
        assertEquals(1, bobLogs.size());
    }

    @Test
    void shouldLimitAuditLogs() {
        IntStream.range(0, 100).forEach(i -> {
            AuditLog log = AuditLog.builder()
                    .userId("user" + i)
                    .action("ACTION_" + i)
                    .resourceType("TEST")
                    .build();
            backOfficeService.logAudit(log);
        });

        List<AuditLog> allLogs = backOfficeService.getAuditLogs(null, null, 100);
        assertEquals(100, allLogs.size());

        List<AuditLog> limitedLogs = backOfficeService.getAuditLogs(null, null, 50);
        assertEquals(50, limitedLogs.size());
    }

    @Test
    void shouldCreateReport() {
        Report report = Report.builder()
                .id(UUID.randomUUID())
                .name("Daily Settlement")
                .description("Settlement report for today")
                .reportType(Report.ReportType.SETTLEMENT)
                .generatedBy("admin")
                .build();

        Report result = backOfficeService.createReport(report);

        assertEquals(Report.Status.PENDING, result.getStatus());
        assertEquals("Daily Settlement", result.getName());
        assertEquals(Report.ReportType.SETTLEMENT, result.getReportType());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldGenerateReport() {
        Report report = Report.builder()
                .id(UUID.randomUUID())
                .name("Fraud Analysis")
                .reportType(Report.ReportType.FRAUD)
                .generatedBy("analyst")
                .build();

        backOfficeService.createReport(report);
        Report generated = backOfficeService.generateReport(report.getId());

        assertEquals(Report.Status.COMPLETED, generated.getStatus());
        assertNotNull(generated.getGeneratedAt());
    }

    @Test
    void shouldGetReportsByType() {
        Report r1 = Report.builder().id(UUID.randomUUID()).name("Txn Report").reportType(Report.ReportType.TRANSACTION).build();
        Report r2 = Report.builder().id(UUID.randomUUID()).name("Settlement Report").reportType(Report.ReportType.SETTLEMENT).build();
        Report r3 = Report.builder().id(UUID.randomUUID()).name("Fraud Report").reportType(Report.ReportType.FRAUD).build();

        backOfficeService.createReport(r1);
        backOfficeService.createReport(r2);
        backOfficeService.createReport(r3);

        List<Report> settlementReports = backOfficeService.getReportsByType("SETTLEMENT");
        assertEquals(1, settlementReports.size());
        assertEquals(Report.ReportType.SETTLEMENT, settlementReports.get(0).getReportType());

        List<Report> allReports = backOfficeService.getReportsByType("TRANSACTION");
        assertEquals(1, allReports.size());
    }

    @Test
    void shouldCreateMonitoringEvent() {
        MonitoringEvent event = backOfficeService.createMonitoringEvent(
                "CPU_USAGE", "WARNING", "server-01",
                "CPU usage exceeded 80%", 85.0, 80.0);

        assertNotNull(event.getId());
        assertEquals("CPU_USAGE", event.getEventType());
        assertEquals(MonitoringEvent.Severity.WARNING, event.getSeverity());
        assertEquals("server-01", event.getSource());
        assertEquals("CPU usage exceeded 80%", event.getMessage());
        assertEquals(85.0, event.getMetricValue());
        assertEquals(80.0, event.getThresholdValue());
        assertFalse(event.getAcknowledged());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void shouldAcknowledgeEvent() {
        MonitoringEvent event = backOfficeService.createMonitoringEvent(
                "DISK_USAGE", "CRITICAL", "server-02",
                "Disk usage at 95%", 95.0, 90.0);

        MonitoringEvent acknowledged = backOfficeService.acknowledgeEvent(event.getId(), "operator1");

        assertTrue(acknowledged.getAcknowledged());
        assertEquals("operator1", acknowledged.getAcknowledgedBy());
        assertNotNull(acknowledged.getAcknowledgedAt());
    }

    @Test
    void shouldGetActiveAlerts() {
        backOfficeService.createMonitoringEvent("CPU", "CRITICAL", "srv1", "CPU overload", 95.0, 90.0);
        backOfficeService.createMonitoringEvent("MEM", "ERROR", "srv2", "Memory high", 88.0, 85.0);
        backOfficeService.createMonitoringEvent("DISK", "INFO", "srv3", "Disk usage normal", 50.0, 80.0);
        MonitoringEvent warning = backOfficeService.createMonitoringEvent(
                "NET", "WARNING", "srv4", "Latency high", 200.0, 150.0);
        MonitoringEvent ackCritical = backOfficeService.createMonitoringEvent(
                "DB", "CRITICAL", "srv5", "DB down", 0.0, 1.0);
        backOfficeService.acknowledgeEvent(ackCritical.getId(), "admin");

        List<MonitoringEvent> activeAlerts = backOfficeService.getActiveAlerts();

        assertEquals(2, activeAlerts.size());
        assertTrue(activeAlerts.stream().allMatch(
                e -> e.getSeverity() == MonitoringEvent.Severity.CRITICAL
                        || e.getSeverity() == MonitoringEvent.Severity.ERROR));
        assertTrue(activeAlerts.stream().noneMatch(MonitoringEvent::getAcknowledged));
    }

    @Test
    void shouldGetMonitoringStats() {
        backOfficeService.createMonitoringEvent("CPU", "CRITICAL", "srv1", "CPU at 100%", 100.0, 90.0);
        backOfficeService.createMonitoringEvent("MEM", "ERROR", "srv2", "Memory leak", 92.0, 85.0);
        backOfficeService.createMonitoringEvent("DISK", "WARNING", "srv3", "Disk 85%", 85.0, 80.0);
        backOfficeService.createMonitoringEvent("NET", "INFO", "srv4", "Normal", 10.0, 50.0);

        Map<MonitoringEvent.Severity, Long> stats = backOfficeService.getMonitoringStats(60);

        assertEquals(4, stats.size());
        assertEquals(1L, stats.get(MonitoringEvent.Severity.CRITICAL));
        assertEquals(1L, stats.get(MonitoringEvent.Severity.ERROR));
        assertEquals(1L, stats.get(MonitoringEvent.Severity.WARNING));
        assertEquals(1L, stats.get(MonitoringEvent.Severity.INFO));
    }

    @Test
    void shouldThrowWhenGeneratingNonexistentReport() {
        assertThrows(IllegalArgumentException.class, () ->
                backOfficeService.generateReport(UUID.randomUUID()));
    }

    @Test
    void shouldThrowWhenAcknowledgingNonexistentEvent() {
        assertThrows(IllegalArgumentException.class, () ->
                backOfficeService.acknowledgeEvent(999L, "admin"));
    }
}
