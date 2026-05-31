package com.switchplatform.platform.controller.clearing;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.InterchangeFee;
import com.switchplatform.platform.model.clearing.InterchangeResult;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.clearing.InterchangeService;
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
    public ResponseEntity<Void> configureInterchange(@Valid @RequestBody Map<String, Object> body) {
        String brand = (String) body.get("brand");
        String cardType = (String) body.get("cardType");
        String region = (String) body.get("region");
        String mcc = (String) body.get("mcc");
        BigDecimal flatFee = new BigDecimal(body.get("flatFee").toString());
        BigDecimal percentageFee = new BigDecimal(body.get("percentageFee").toString());
        interchangeService.configureFee(brand, cardType, region, mcc, flatFee, percentageFee);
        return ResponseEntity.ok().build();
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
