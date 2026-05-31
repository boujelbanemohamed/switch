package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final Map<UUID, AuditLog> auditLogs = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> resourceIndex = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> userIndex = new ConcurrentHashMap<>();

    public AuditLog record(String action, String resourceType, String resourceId,
                           String details, String status, String username, UUID userId,
                           HttpServletRequest request) {
        AuditLog logEntry = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .username(username)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .status(status != null ? status : "SUCCESS")
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .createdAt(OffsetDateTime.now())
                .build();

        auditLogs.put(logEntry.getId(), logEntry);
        resourceIndex.computeIfAbsent(resourceType + ":" + resourceId, k -> Collections.synchronizedList(new ArrayList<>())).add(logEntry.getId());
        if (userId != null) userIndex.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(logEntry.getId());

        log.info("Audit: action={} resource={}:{} user={} status={}", action, resourceType, resourceId, username, status);
        return logEntry;
    }

    public AuditLog record(String action, String resourceType, String resourceId,
                           String details, String username, UUID userId) {
        return record(action, resourceType, resourceId, details, "SUCCESS", username, userId, null);
    }

    public List<AuditLog> listByResource(String resourceType, String resourceId, int limit) {
        List<UUID> ids = resourceIndex.getOrDefault(resourceType + ":" + resourceId, Collections.emptyList());
        return ids.stream()
                .sorted(Collections.reverseOrder())
                .limit(limit)
                .map(auditLogs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Page<AuditLog> listByResource(String resourceType, String resourceId, int page, int size) {
        List<UUID> ids = resourceIndex.getOrDefault(resourceType + ":" + resourceId, Collections.emptyList());
        List<AuditLog> all = ids.stream()
                .map(auditLogs::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    public List<AuditLog> listByUser(UUID userId, int limit) {
        List<UUID> ids = userIndex.getOrDefault(userId, Collections.emptyList());
        return ids.stream()
                .sorted(Collections.reverseOrder())
                .limit(limit)
                .map(auditLogs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Page<AuditLog> listByUser(UUID userId, int page, int size) {
        List<UUID> ids = userIndex.getOrDefault(userId, Collections.emptyList());
        List<AuditLog> all = ids.stream()
                .map(auditLogs::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    public List<AuditLog> listAll(int limit) {
        return auditLogs.values().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Page<AuditLog> listAll(int page, int size) {
        return listAllFiltered(page, size, null, null, null, null);
    }

    public Page<AuditLog> listAllFiltered(int page, int size, String action, UUID userId,
                                           OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        List<AuditLog> all = auditLogs.values().stream()
                .filter(a -> action == null || a.getAction().equalsIgnoreCase(action))
                .filter(a -> userId == null || userId.equals(a.getUserId()))
                .filter(a -> dateFrom == null || (a.getCreatedAt() != null && !a.getCreatedAt().isBefore(dateFrom)))
                .filter(a -> dateTo == null || (a.getCreatedAt() != null && !a.getCreatedAt().isAfter(dateTo)))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    public List<AuditLog> listByAction(String action, int limit) {
        return auditLogs.values().stream()
                .filter(a -> a.getAction().equalsIgnoreCase(action))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public long countByStatus(String status) {
        return auditLogs.values().stream()
                .filter(a -> a.getStatus().equalsIgnoreCase(status))
                .count();
    }

    private <T> Page<T> paginate(List<T> list, int page, int size) {
        int total = list.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageContent = fromIndex < total ? list.subList(fromIndex, toIndex) : Collections.emptyList();
        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }
}
