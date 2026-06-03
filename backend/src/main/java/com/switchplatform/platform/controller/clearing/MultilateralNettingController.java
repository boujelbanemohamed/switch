package com.switchplatform.platform.controller.clearing;

import com.switchplatform.platform.model.clearing.MultilateralNettingSession;
import com.switchplatform.platform.model.clearing.MultilateralPosition;
import com.switchplatform.platform.service.clearing.MultilateralNettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/netting")
@RequiredArgsConstructor
public class MultilateralNettingController {

    private final MultilateralNettingService nettingService;

    @PostMapping("/calculate")
    public ResponseEntity<MultilateralNettingSession> calculate(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "TND") String currency) {
        LocalDate sessionDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(nettingService.calculateNetting(sessionDate, currency));
    }

    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<MultilateralNettingSession> confirm(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(nettingService.confirmSession(sessionId));
    }

    @PostMapping("/{sessionId}/settle")
    public ResponseEntity<MultilateralNettingSession> settle(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(nettingService.settleSession(sessionId));
    }

    @GetMapping("/{sessionId}/positions")
    public ResponseEntity<List<MultilateralPosition>> getPositions(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(nettingService.getPositions(sessionId));
    }

    @GetMapping("/latest")
    public ResponseEntity<MultilateralNettingSession> getLatest() {
        return nettingService.getLatestSession()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
