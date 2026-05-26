package com.switch.platform.controller.clearing;

import com.switch.platform.model.clearing.ClearingRecord;
import com.switch.platform.model.clearing.NettingRecord;
import com.switch.platform.service.clearing.ClearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clearing")
@RequiredArgsConstructor
public class ClearingController {

    private final ClearingService clearingService;

    @PostMapping("/process")
    public ResponseEntity<ClearingRecord> process(@RequestBody ClearingService.ClearingData data) {
        return ResponseEntity.ok(clearingService.processClearing(data));
    }

    @PostMapping("/{id}/clear")
    public ResponseEntity<ClearingRecord> clear(@PathVariable UUID id) {
        return ResponseEntity.ok(clearingService.clearTransaction(id));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<ClearingRecord> dispute(@PathVariable UUID id, @RequestBody Map<String, String> body) {
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
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clearingService.settleNetting(id, body.get("reference")));
    }
}
