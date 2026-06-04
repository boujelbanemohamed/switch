package com.switchplatform.platform.controller.regulatory;

import com.switchplatform.platform.service.regulatory.RegulatoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/regulatory")
@RequiredArgsConstructor
public class RegulatoryController {

    private final RegulatoryReportService regulatoryReportService;

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> listTemplates() {
        return ResponseEntity.ok(regulatoryReportService.listTemplates());
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<byte[]> generateReport(@RequestBody Map<String, String> body) {
        String templateId = body.get("templateId");
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        LocalDate endDate = LocalDate.parse(body.get("endDate"));
        String format = body.getOrDefault("format", "CSV");

        byte[] content = regulatoryReportService.generateReport(templateId, startDate, endDate, format);
        String filename = templateId + "-" + startDate + "-to-" + endDate + "." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
