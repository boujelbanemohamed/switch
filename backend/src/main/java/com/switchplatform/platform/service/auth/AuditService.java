package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import com.switchplatform.platform.repository.auth.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByResource(String resourceType, String resourceId, int page, int size) {
        List<AuditLog> results = auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId, PageRequest.of(page, size));
        long total = results.size();
        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByUser(UUID userId, int limit) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByUser(UUID userId, int page, int size) {
        List<AuditLog> results = auditLogRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        long total = results.size();
        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listAll(int limit) {
        return auditLogRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(int page, int size) {
        return auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAllFiltered(int page, int size, String action, UUID userId,
                                           OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        long count = auditLogRepository.count();
        List<AuditLog> results = auditLogRepository.findFiltered(
                action, userId, dateFrom, dateTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageImpl<>(results, PageRequest.of(page, size), count);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByAction(String action, int limit) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, PageRequest.of(0, limit))
                .getContent();
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return auditLogRepository.countByStatus(status);
    }
}
