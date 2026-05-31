package com.switchplatform.platform.repository.backoffice;

import com.switchplatform.platform.model.backoffice.MonitoringEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {

    List<MonitoringEvent> findBySeverity(MonitoringEvent.Severity severity);

    Page<MonitoringEvent> findBySeverityOrderByCreatedAtDesc(MonitoringEvent.Severity severity, Pageable pageable);

    Page<MonitoringEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    List<MonitoringEvent> findBySource(String source);

    long countBySeverity(MonitoringEvent.Severity severity);
}
