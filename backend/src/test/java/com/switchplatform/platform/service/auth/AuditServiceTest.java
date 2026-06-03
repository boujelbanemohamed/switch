package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import com.switchplatform.platform.repository.auth.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditService service;
    private AuditLogRepository repository;
    private final Map<UUID, AuditLog> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    @BeforeEach
    void setUp() {
        store.clear();

        repository = mock(AuditLogRepository.class);

        when(repository.save(any())).thenAnswer(inv -> {
            AuditLog a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            if (a.getCreatedAt() == null) a.setCreatedAt(OffsetDateTime.now());
            store.put(a.getId(), a);
            return a;
        });
        when(repository.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable p = inv.getArgument(0);
            List<AuditLog> all = new ArrayList<>(store.values());
            all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), all.size());
            List<AuditLog> content = start < all.size() ? all.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, all.size());
        });
        when(repository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(any(), any(), any())).thenAnswer(inv -> {
            String rt = inv.getArgument(0);
            String ri = inv.getArgument(1);
            Pageable p = inv.getArgument(2);
            return store.values().stream()
                    .filter(a -> rt.equals(a.getResourceType()) && ri.equals(a.getResourceId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .skip(p.getOffset())
                    .limit(p.getPageSize())
                    .toList();
        });
        when(repository.findByUserIdOrderByCreatedAtDesc(any(), any())).thenAnswer(inv -> {
            UUID uid = inv.getArgument(0);
            Pageable p = inv.getArgument(1);
            return store.values().stream()
                    .filter(a -> uid.equals(a.getUserId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .skip(p.getOffset())
                    .limit(p.getPageSize())
                    .toList();
        });
        when(repository.findByActionOrderByCreatedAtDesc(any(), any())).thenAnswer(inv -> {
            String action = inv.getArgument(0);
            Pageable p = inv.getArgument(1);
            List<AuditLog> filtered = store.values().stream()
                    .filter(a -> action.equalsIgnoreCase(a.getAction()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), filtered.size());
            List<AuditLog> content = start < filtered.size() ? filtered.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, filtered.size());
        });
        when(repository.findFiltered(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            String action = inv.getArgument(0);
            UUID userId = inv.getArgument(1);
            OffsetDateTime dateFrom = inv.getArgument(2);
            OffsetDateTime dateTo = inv.getArgument(3);
            Pageable p = inv.getArgument(4);
            return store.values().stream()
                    .filter(a -> action == null || a.getAction().equalsIgnoreCase(action))
                    .filter(a -> userId == null || userId.equals(a.getUserId()))
                    .filter(a -> dateFrom == null || (a.getCreatedAt() != null && !a.getCreatedAt().isBefore(dateFrom)))
                    .filter(a -> dateTo == null || (a.getCreatedAt() != null && !a.getCreatedAt().isAfter(dateTo)))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .skip(p.getOffset())
                    .limit(p.getPageSize())
                    .toList();
        });
        when(repository.count()).thenAnswer(inv -> (long) store.size());
        when(repository.countByStatus(any())).thenAnswer(inv -> {
            String status = inv.getArgument(0);
            return store.values().stream().filter(a -> a.getStatus().equalsIgnoreCase(status)).count();
        });

        service = new AuditService(repository);
    }

    private AuditLog logEntry(String action, String resourceType, String resourceId,
                                        String status, UUID userId) {
        AuditLog a = AuditLog.builder()
                .action(action).resourceType(resourceType).resourceId(resourceId)
                .status(status).userId(userId).username("testuser")
                .createdAt(OffsetDateTime.now().plusNanos(counter.incrementAndGet()))
                .build();
        return repository.save(a);
    }

    @Test
    void shouldRecordAuditLog() {
        AuditLog logged = service.record("LOGIN", "USER", "1", "details",
                "SUCCESS", "testuser", UUID.randomUUID(), null);
        assertNotNull(logged.getId());
        assertEquals("LOGIN", logged.getAction());
    }

    @Test
    void shouldListByResourceWithLimit() {
        UUID uid = UUID.randomUUID();
        logEntry("VIEW", "CARD", "123", "SUCCESS", uid);
        logEntry("VIEW", "CARD", "123", "SUCCESS", uid);
        logEntry("VIEW", "CARD", "456", "SUCCESS", uid);
        List<AuditLog> results = service.listByResource("CARD", "123", 10);
        assertEquals(2, results.size());
    }

    @Test
    void shouldListByUserWithLimit() {
        UUID uid = UUID.randomUUID();
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", uid);
        logEntry("LOGOUT", "AUTH", "1", "SUCCESS", uid);
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        List<AuditLog> results = service.listByUser(uid, 10);
        assertEquals(2, results.size());
    }

    @Test
    void shouldListAllWithLimit() {
        for (int i = 0; i < 5; i++) {
            logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        }
        List<AuditLog> results = service.listAll(3);
        assertEquals(3, results.size());
    }

    @Test
    void shouldListAllPaginated() {
        for (int i = 0; i < 10; i++) {
            logEntry("VIEW", "REPORT", String.valueOf(i), "SUCCESS", UUID.randomUUID());
        }
        Page<AuditLog> page = service.listAll(0, 4);
        assertEquals(4, page.getContent().size());
        assertEquals(10, page.getTotalElements());
    }

    @Test
    void shouldListByAction() {
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        logEntry("LOGOUT", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        List<AuditLog> results = service.listByAction("LOGIN", 10);
        assertEquals(2, results.size());
    }

    @Test
    void shouldListFiltered() {
        UUID uid = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            logEntry("LOGIN", "AUTH", "1", "SUCCESS", uid);
            logEntry("VIEW", "REPORT", "2", "SUCCESS", UUID.randomUUID());
        }
        Page<AuditLog> page = service.listAllFiltered(0, 10, "LOGIN", uid, null, null);
        assertEquals(5, page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(a -> "LOGIN".equals(a.getAction())));
        assertTrue(page.getContent().stream().allMatch(a -> uid.equals(a.getUserId())));
    }

    @Test
    void shouldCountByStatus() {
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        logEntry("LOGIN", "AUTH", "1", "FAILED", UUID.randomUUID());
        logEntry("LOGIN", "AUTH", "1", "SUCCESS", UUID.randomUUID());
        assertEquals(2, service.countByStatus("SUCCESS"));
    }

    @Test
    void shouldListByResourcePaginated() {
        for (int i = 0; i < 6; i++) {
            logEntry("VIEW", "CARD", "999", "SUCCESS", UUID.randomUUID());
        }
        Page<AuditLog> page = service.listByResource("CARD", "999", 0, 4);
        assertEquals(4, page.getContent().size());
    }

    @Test
    void shouldListByUserPaginated() {
        UUID uid = UUID.randomUUID();
        for (int i = 0; i < 7; i++) {
            logEntry("LOGIN", "AUTH", "1", "SUCCESS", uid);
        }
        Page<AuditLog> page = service.listByUser(uid, 0, 3);
        assertEquals(3, page.getContent().size());
    }
}
