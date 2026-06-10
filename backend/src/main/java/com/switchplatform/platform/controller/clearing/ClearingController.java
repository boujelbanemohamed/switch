package com.switchplatform.platform.controller.clearing;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.InterchangeFee;
import com.switchplatform.platform.model.clearing.InterchangeResult;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.clearing.InterchangeService;
import com.switchplatform.platform.service.clearing.SettlementFileService;
import com.switchplatform.platform.service.clearing.bct.BctSettlementService;
import com.switchplatform.platform.service.clearing.reporting.SchemeReportService;
import com.switchplatform.platform.repository.clearing.ReconciliationRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clearing")
@RequiredArgsConstructor
@Validated
public class ClearingController {

    private final ClearingService clearingService;
    private final InterchangeService interchangeService;
    private final SettlementFileService settlementFileService;
    private final BctSettlementService bctSettlementService;
    private final SchemeReportService schemeReportService;
    private final ParticipantRepository participantRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;

    @PostMapping("/process")
    public ResponseEntity<ClearingRecord> process(@Valid @RequestBody ClearingService.ClearingData data) {
        return ResponseEntity.ok(clearingService.processClearing(data));
    }

    @PostMapping("/{id}/clear")
    public ResponseEntity<ClearingRecord> clear(@PathVariable UUID id) {
        return ResponseEntity.ok(clearingService.clearTransaction(id));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<ClearingRecord> dispute(@PathVariable UUID id, @Valid @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clearingService.disputeClearing(id, body.get("reason")));
    }

    @GetMapping("/by-date/{date}")
    public ResponseEntity<List<ClearingRecord>> getByDate(@PathVariable String date) {
        return ResponseEntity.ok(clearingService.getClearingByDate(LocalDate.parse(date)));
    }

    @GetMapping("/by-participant/{participantId}")
    public ResponseEntity<List<ClearingRecord>> getByParticipant(@PathVariable UUID participantId) {
        return ResponseEntity.ok(clearingService.getClearingByParticipant(participantId));
    }

    @PostMapping("/netting/calculate")
    public ResponseEntity<List<NettingRecord>> calculateNetting(@RequestParam String date) {
        return ResponseEntity.ok(clearingService.calculateNetting(LocalDate.parse(date)));
    }

    @PostMapping("/netting/{id}/confirm")
    public ResponseEntity<NettingRecord> confirmNetting(@PathVariable UUID id) {
        return ResponseEntity.ok(clearingService.confirmNetting(id));
    }

    @PostMapping("/netting/{id}/settle")
    public ResponseEntity<NettingRecord> settleNetting(
            @PathVariable UUID id, @Valid @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clearingService.settleNetting(id, body.get("reference")));
    }

    @PostMapping("/interchange/configure")
    public ResponseEntity<Void> configureInterchange(@RequestBody Map<String, Object> body) {
        String brand = (String) body.get("brand");
        String cardType = (String) body.get("cardType");
        String region = (String) body.get("region");
        String mcc = (String) body.get("mcc");
        BigDecimal flatFee = new BigDecimal(body.get("flatFee").toString());
        BigDecimal percentageFee = new BigDecimal(body.get("percentageFee").toString());
        interchangeService.configureFee(brand, cardType, region, mcc, flatFee, percentageFee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/interchange")
    public ResponseEntity<List<InterchangeFee>> listInterchangeFees() {
        return ResponseEntity.ok(interchangeService.listAllFees());
    }

    @PutMapping("/interchange/{id}")
    public ResponseEntity<InterchangeFee> updateInterchangeFee(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String brand = (String) body.get("brand");
        String cardType = (String) body.get("cardType");
        String region = (String) body.get("region");
        String mcc = (String) body.get("mcc");
        BigDecimal flatFee = body.get("flatFee") != null ? new BigDecimal(body.get("flatFee").toString()) : null;
        BigDecimal percentageFee = body.get("percentageFee") != null ? new BigDecimal(body.get("percentageFee").toString()) : null;
        return ResponseEntity.ok(interchangeService.updateFee(id, brand, cardType, region, mcc, flatFee, percentageFee));
    }

    @DeleteMapping("/interchange/{id}")
    public ResponseEntity<Void> deleteInterchangeFee(@PathVariable UUID id) {
        interchangeService.deleteFee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/outgoing")
    public ResponseEntity<String> downloadOutgoingFile(
            @RequestParam String date,
            @RequestParam UUID participantId,
            @RequestParam(defaultValue = "CSV") String format) {
        String content = settlementFileService.generateOutgoingClearingFile(LocalDate.parse(date), participantId, format);
        String ext = switch (format.toUpperCase()) {
            case "COMPCONF" -> "cmp";
            case "CP50" -> "cp5";
            default -> format.toLowerCase();
        };
        String filename = "clearing-" + date + "-" + participantId + "." + ext;
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    @PostMapping("/files/incoming")
    public ResponseEntity<SettlementFileService.ReconciliationResult> uploadIncomingFile(
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String format = body.getOrDefault("format", "CSV");
        UUID participantId = body.containsKey("participantId") ? UUID.fromString(body.get("participantId")) : null;
        if (participantId == null) {
            var def = participantRepository.findByCode("SWITCH");
            participantId = def.map(Participant::getId).orElse(null);
        }
        return ResponseEntity.ok(settlementFileService.ingestIncomingClearingFile(content, format, participantId));
    }

    @GetMapping("/files/bct")
    public ResponseEntity<String> downloadBctSettlementFile(@RequestParam String date) {
        String content = bctSettlementService.generateBctSettlementFile(LocalDate.parse(date));
        String filename = "bct-settlement-" + date + ".csv";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<List<ReconciliationRecord>> listReconciliationRecords(
            @RequestParam(required = false) String date) {
        if (date != null) {
            return ResponseEntity.ok(reconciliationRecordRepository.findByReconciliationDate(LocalDate.parse(date)));
        }
        return ResponseEntity.ok(reconciliationRecordRepository.findAll());
    }

    @GetMapping("/reports/quarterly")
    public ResponseEntity<String> quarterlyReport(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam(defaultValue = "ALL") String scheme) {
        return ResponseEntity.ok(schemeReportService.generateQuarterlyReport(year, quarter, scheme));
    }

    @GetMapping("/interchange/calculate")
    public ResponseEntity<InterchangeResult> calculateInterchange(
            @RequestParam String brand,
            @RequestParam String cardType,
            @RequestParam String region,
            @RequestParam(required = false) String mcc,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(
                interchangeService.calculateInterchange(brand, cardType, region, mcc, amount, currency));
    }
}
