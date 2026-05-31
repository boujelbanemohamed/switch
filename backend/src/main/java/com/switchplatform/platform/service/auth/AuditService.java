package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import com.switchplatform.platform.repository.auth.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
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

        auditLogRepository.save(logEntry);

        log.info("Audit: action={} resource={}:{} user={} status={}", action, resourceType, resourceId, username, status);
        return logEntry;
    }

    public AuditLog record(String action, String resourceType, String resourceId,
                           String details, String username, UUID userId) {
        return record(action, resourceType, resourceId, details, "SUCCESS", username, userId, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByResource(String resourceType, String resourceId, int limit) {
        return auditLogRepository.findAll().stream()
                .filter(a -> resourceType.equals(a.getResourceType()) && resourceId.equals(a.getResourceId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByResource(String resourceType, String resourceId, int page, int size) {
        List<AuditLog> all = auditLogRepository.findAll().stream()
                .filter(a -> resourceType.equals(a.getResourceType()) && resourceId.equals(a.getResourceId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByUser(UUID userId, int limit) {
        return auditLogRepository.findAll().stream()
                .filter(a -> userId.equals(a.getUserId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByUser(UUID userId, int page, int size) {
        List<AuditLog> all = auditLogRepository.findAll().stream()
                .filter(a -> userId.equals(a.getUserId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listAll(int limit) {
        return auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(int page, int size) {
        return listAllFiltered(page, size, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAllFiltered(int page, int size, String action, UUID userId,
                                           OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        List<AuditLog> all = auditLogRepository.findAll().stream()
                .filter(a -> action == null || a.getAction().equalsIgnoreCase(action))
                .filter(a -> userId == null || userId.equals(a.getUserId()))
                .filter(a -> dateFrom == null || (a.getCreatedAt() != null && !a.getCreatedAt().isBefore(dateFrom)))
                .filter(a -> dateTo == null || (a.getCreatedAt() != null && !a.getCreatedAt().isAfter(dateTo)))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        return paginate(all, page, size);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByAction(String action, int limit) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, PageRequest.of(0, limit))
                .getContent();
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return auditLogRepository.findAll().stream()
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
