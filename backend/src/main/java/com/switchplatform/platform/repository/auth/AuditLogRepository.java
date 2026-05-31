package com.switchplatform.platform.repository.auth;

import com.switchplatform.platform.model.auth.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository("authAuditLogRepository")
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
