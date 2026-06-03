package com.switchplatform.platform.service.backoffice;

import com.switchplatform.platform.model.backoffice.AuditLog;
import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import com.switchplatform.platform.model.backoffice.Report;
import com.switchplatform.platform.repository.backoffice.AuditLogRepository;
import com.switchplatform.platform.repository.backoffice.MonitoringEventRepository;
import com.switchplatform.platform.repository.backoffice.ReportRepository;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.fraud.FraudAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackOfficeServiceTest {

    private BackOfficeService backOfficeService;
    private final ConcurrentHashMap<Long, AuditLog> auditLogStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Report> reportStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, MonitoringEvent> monitoringEventStore = new ConcurrentHashMap<>();
    private final AtomicLong auditLogIdGen = new AtomicLong(1);
    private final AtomicLong monitoringEventIdGen = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        auditLogStore.clear();
        reportStore.clear();
        monitoringEventStore.clear();

        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ReportRepository reportRepository = mock(ReportRepository.class);
        MonitoringEventRepository monitoringEventRepository = mock(MonitoringEventRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        ClearingRecordRepository clearingRecordRepository = mock(ClearingRecordRepository.class);
        FraudAlertRepository fraudAlertRepository = mock(FraudAlertRepository.class);

        when(auditLogRepository.save(any())).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            if (log.getId() == null) log.setId(auditLogIdGen.getAndIncrement());
            if (log.getCreatedAt() == null) log.setCreatedAt(OffsetDateTime.now());
            auditLogStore.put(log.getId(), log);
            return log;
        });
        when(auditLogRepository.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable pageable = inv.getArgument(0);
            List<AuditLog> all = new ArrayList<>(auditLogStore.values());
            all.sort(Comparator.comparing(AuditLog::getCreatedAt,
                    java.util.Comparator.nullsLast(Comparator.reverseOrder())));
            int end = Math.min(pageable.getPageSize(), all.size());
            return new PageImpl<>(all.subList(0, end));
        });

        when(reportRepository.save(any())).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            if (r.getCreatedAt() == null) r.setCreatedAt(OffsetDateTime.now());
            reportStore.put(r.getId(), r);
            return r;
        });
        when(reportRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(reportStore.get(inv.getArgument(0))));
        when(reportRepository.findByReportType(any())).thenAnswer(inv -> {
            Report.ReportType type = inv.getArgument(0);
            return reportStore.values().stream()
                    .filter(r -> type == r.getReportType()).toList();
        });

        when(monitoringEventRepository.save(any())).thenAnswer(inv -> {
            MonitoringEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(monitoringEventIdGen.getAndIncrement());
            if (e.getCreatedAt() == null) e.setCreatedAt(OffsetDateTime.now());
            monitoringEventStore.put(e.getId(), e);
            return e;
        });
        when(monitoringEventRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(monitoringEventStore.get(inv.getArgument(0))));
        when(monitoringEventRepository.findBySeverityOrderByCreatedAtDesc(any(), any())).thenAnswer(inv -> {
            MonitoringEvent.Severity sev = inv.getArgument(0);
            List<MonitoringEvent> filtered = monitoringEventStore.values().stream()
                    .filter(e -> e.getSeverity() == sev)
                    .sorted(Comparator.comparing(MonitoringEvent::getCreatedAt,
                            java.util.Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            return new PageImpl<>(filtered);
        });
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(clearingRecordRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(fraudAlertRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        when(monitoringEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any()))
                .thenAnswer(inv -> {
                    OffsetDateTime start = inv.getArgument(0);
                    OffsetDateTime end = inv.getArgument(1);
                    List<MonitoringEvent> filtered = monitoringEventStore.values().stream()
                            .filter(e -> e.getCreatedAt() != null
                                    && !e.getCreatedAt().isBefore(start)
                                    && !e.getCreatedAt().isAfter(end))
                            .sorted(Comparator.comparing(MonitoringEvent::getCreatedAt,
                                    java.util.Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                    return new PageImpl<>(filtered);
                });

        backOfficeService = new BackOfficeService(auditLogRepository, reportRepository, monitoringEventRepository,
                transactionRepository, clearingRecordRepository, fraudAlertRepository);
        try {
            var f = BackOfficeService.class.getDeclaredField("reportsDirectory");
            f.setAccessible(true);
            f.set(backOfficeService, System.getProperty("java.io.tmpdir"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
