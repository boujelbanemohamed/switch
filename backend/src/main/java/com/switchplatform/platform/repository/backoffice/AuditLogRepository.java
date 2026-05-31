package com.switchplatform.platform.repository.backoffice;

import com.switchplatform.platform.model.backoffice.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
