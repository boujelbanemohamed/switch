package com.switchplatform.platform.repository.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository("authAuditLogRepository")
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, String resourceId, Pageable pageable);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR LOWER(a.action) = LOWER(:action)) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.createdAt <= :dateTo) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findFiltered(String action, UUID userId,
                                OffsetDateTime dateFrom, OffsetDateTime dateTo, Pageable pageable);

    long countByStatus(String status);
}
