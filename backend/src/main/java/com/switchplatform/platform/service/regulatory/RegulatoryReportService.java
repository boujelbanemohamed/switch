package com.switchplatform.platform.service.regulatory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegulatoryReportService {

    public List<Map<String, Object>> listTemplates() {
        return List.of(
                Map.of("id", "bct-daily", "name", "BCT Daily Transaction Report", "periodicity", "DAILY",
                        "description", "Daily transaction summary for Banque Centrale de Tunisie"),
                Map.of("id", "bct-monthly", "name", "BCT Monthly Statistics", "periodicity", "MONTHLY",
                        "description", "Monthly statistics on transaction volumes and values"),
                Map.of("id", "sibtel-daily", "name", "SIBTEL Daily Switch Report", "periodicity", "DAILY",
                        "description", "Daily switch activity report for SIBTEL"),
                Map.of("id", "sibtel-monthly", "name", "SIBTEL Monthly Settlement", "periodicity", "MONTHLY",
                        "description", "Monthly settlement summary for SIBTEL participants"));
    }

    public byte[] generateReport(String templateId, LocalDate startDate, LocalDate endDate, String format) {
        log.info("Generating report: template={}, from={}, to={}, format={}",
                templateId, startDate, endDate, format);
        StringBuilder sb = new StringBuilder();
        sb.append("Report: ").append(templateId).append("\n");
        sb.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        sb.append("Generated: ").append(LocalDate.now().format(DateTimeFormatter.ISO_DATE)).append("\n");
        sb.append("---\n");
        sb.append("template_id,period_start,period_end,generated_at\n");
        sb.append(templateId).append(",")
          .append(startDate).append(",")
          .append(endDate).append(",")
          .append(LocalDate.now()).append("\n");
        return sb.toString().getBytes();
    }
}
