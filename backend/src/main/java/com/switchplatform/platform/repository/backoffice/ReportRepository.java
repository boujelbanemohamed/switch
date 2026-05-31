package com.switchplatform.platform.repository.backoffice;

import com.switchplatform.platform.model.backoffice.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findByName(String name);

    List<Report> findByReportType(Report.ReportType reportType);

    List<Report> findByStatus(Report.Status status);

    List<Report> findByGeneratedAtBetweenOrderByGeneratedAtDesc(
            OffsetDateTime start, OffsetDateTime end);
}
